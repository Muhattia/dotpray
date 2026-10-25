import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JVM audit of the localized resources, fonts, layouts and widget RTL rules.
 *
 * <p>This test deliberately does not need an Android device. It reads the
 * resource XML directly, so a missing Arabic string, a broken plural argument
 * position or a hardcoded label fails before the APK is built. Run from the
 * project root:
 * {@code javac --release 8 -encoding UTF-8 -d build/test tools/TestLocale.java}
 * then {@code java -Dfile.encoding=UTF-8 -cp build/test TestLocale}.
 */
public final class TestLocale {

    private static final File RES = new File("app/src/main/res");
    private static final File VALUES = new File(RES, "values");
    private static final File VALUES_AR = new File(RES, "values-ar");
    private static final File JAVA = new File("app/src/main/java");
    private static final Pattern ARABIC = Pattern.compile("[\u0600-\u06FF]");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+)\\$[a-zA-Z]");

    private static final Map<String, String> EN = new LinkedHashMap<>();
    private static final Map<String, String> AR = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> EN_PLURALS =
            new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> AR_PLURALS =
            new LinkedHashMap<>();

    private static int checks;
    private static int failures;

    private TestLocale() {
    }

    public static void main(String[] args) throws Exception {
        strings("values", EN, EN_PLURALS);
        strings("values-ar", AR, AR_PLURALS);
        check("English strings loaded", EN.size() > 100);
        check("Arabic strings loaded", AR.size() > 100);

        stringParity();
        placeholders();
        arabicCopy();
        arabicPlurals();
        arrays();
        fonts();
        layouts();
        scales();
        widgetMetadata();
        widgetSettings();
        javaCopy();
        manifest();
        rtl();

        System.out.println();
        if (failures == 0) {
            System.out.println("PASS " + checks + " checks");
        } else {
            System.out.println("FAIL " + failures + " of " + checks + " checks");
            System.exit(1);
        }
    }

    // ------------------------------------------------------------- parsing

    private static void strings(String folder, Map<String, String> strings,
                                Map<String, Map<String, String>> plurals)
            throws Exception {
        File file = new File(new File(RES, folder), "strings.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(file);
        Element root = doc.getDocumentElement();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element)) continue;
            Element element = (Element) node;
            String name = element.getAttribute("name");
            if ("string".equals(element.getTagName())) {
                strings.put(name, element.getTextContent());
            } else if ("plurals".equals(element.getTagName())) {
                Map<String, String> items = new LinkedHashMap<>();
                NodeList itemsList = element.getChildNodes();
                for (int j = 0; j < itemsList.getLength(); j++) {
                    Node itemNode = itemsList.item(j);
                    if (!(itemNode instanceof Element)) continue;
                    Element item = (Element) itemNode;
                    if ("item".equals(item.getTagName())) {
                        items.put(item.getAttribute("quantity"), item.getTextContent());
                    }
                }
                plurals.put(name, items);
            }
        }
    }

    private static boolean translatable(File file, String key) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(file);
        NodeList strings = doc.getElementsByTagName("string");
        for (int i = 0; i < strings.getLength(); i++) {
            Element element = (Element) strings.item(i);
            if (key.equals(element.getAttribute("name"))) {
                return !"false".equals(element.getAttribute("translatable"));
            }
        }
        return true;
    }

    // -------------------------------------------------------------- checks

    private static void stringParity() throws Exception {
        section("string parity");
        File baseFile = new File(VALUES, "strings.xml");
        List<String> missing = new ArrayList<>();
        for (String key : EN.keySet()) {
            if (!translatable(baseFile, key)) continue;
            if (!AR.containsKey(key)) missing.add(key);
        }
        check("every translatable English string has Arabic: " + missing,
                missing.isEmpty());

        for (String key : AR.keySet()) {
            check("Arabic key exists in English: " + key, EN.containsKey(key));
        }

        for (String key : EN_PLURALS.keySet()) {
            check("Arabic plural exists: " + key, AR_PLURALS.containsKey(key));
        }
        for (String key : AR_PLURALS.keySet()) {
            check("English plural exists: " + key, EN_PLURALS.containsKey(key));
        }
    }

    private static void placeholders() {
        section("format arguments");
        for (String key : EN.keySet()) {
            if (!AR.containsKey(key)) continue;
            Set<String> expected = placeholders(EN.get(key));
            Set<String> actual = placeholders(AR.get(key));
            eq("placeholders match for " + key, expected, actual);
        }
        eq("English elapsed full arguments",
                new TreeSet<>(Arrays.asList("1", "2")),
                placeholders(EN_PLURALS.get("elapsed_minutes").get("other")));
    }

    private static void arabicCopy() throws Exception {
        section("Arabic copy");
        containsArabic("app language", AR.get("app_language"));
        containsArabic("settings title", AR.get("settings_title"));
        containsArabic("theme label", AR.get("theme_label"));
        containsArabic("alerts label", AR.get("alerts_label"));
        containsArabic("no prayer header", AR.get("prayer_now"));
        containsArabic("next prayer header", AR.get("next_prayer"));
        containsArabic("tomorrow label", AR.get("tomorrow_label"));
        containsArabic("qibla", AR.get("qibla"));
        containsArabic("Fajr", AR.get("prayer_fajr"));
        containsArabic("Dhuhr", AR.get("prayer_dhuhr"));
        containsArabic("Asr", AR.get("prayer_asr"));
        containsArabic("Maghrib", AR.get("prayer_maghrib"));
        containsArabic("Isha", AR.get("prayer_isha"));
        notEqual("app language is translated", EN.get("app_language"),
                AR.get("app_language"));
        notEqual("Arabic label is Arabic", "Arabic", AR.get("app_language"));
        eq("Arabic language pill", "العربية", EN.get("language_arabic"));
        eq("English language pill", "English", EN.get("language_english"));
        eq("English default remains English", "App Language", EN.get("app_language"));
        eq("Add Widget uses the agreed term", "أضف ويدجت", AR.get("add_widget"));
        eq("Widget section uses the agreed plural", "الويدجتات",
                AR.get("widgets_label"));
        check("no obsolete widget term in Arabic",
                !AR.toString().contains("ودجة") && !AR.toString().contains("الودجات")
                        && !AR.toString().contains("والودجتين"));
        check("Arabic theme note covers all widgets",
                AR.get("theme_applies").contains("جميع الويدجتات"));
        containsArabic("developer credit", AR.get("about_credits"));
        check("developer credit names Muhammad Attia",
                EN.get("about_credits").contains("MUHAMMAD ATTIA")
                        && AR.get("about_credits").contains("محمد عطية"));
        check("developer credit omits the removed collaborator",
                !EN.get("about_credits").contains("HEMDAN")
                        && !AR.get("about_credits").contains("حمدان"));
        check("About body omits the current location label",
                !EN.get("about_body").contains("CURRENT LOCATION")
                        && !AR.get("about_body").contains("الموقع الحالي"));
        check("About body keeps the typeface credit",
                EN.get("about_body").contains("TYPEFACES: DOTO")
                        && AR.get("about_body").contains("الخطوط: DOTO"));
        check("home footer version drops the typeface credit",
                !EN.get("version_label").contains("DOTO")
                        && !EN.get("version_label").contains("ALEXANDRIA")
                        && !AR.get("version_label").contains("DOTO")
                        && !AR.get("version_label").contains("ALEXANDRIA"));
        check("Arabic minimal widget is named بسيط",
                AR.get("widget_minimal_label").contains("بسيط")
                        && AR.get("widget_minimal_button").contains("بسيط")
                        && AR.get("widget_size_minimal").contains("بسيط"));
        check("Arabic minimal widget drops مصغر",
                !AR.get("widget_minimal_label").contains("مصغر")
                        && !AR.get("widget_minimal_button").contains("مصغر")
                        && !AR.get("widget_size_minimal").contains("مصغر"));
        check("Arabic widget strings stay on Alexandria",
                read(new File(VALUES_AR, "styles.xml")).contains("@font/alexandria_"));
        eq("Arabic name is Alexandria", true,
                read(new File(VALUES_AR, "styles.xml")).contains("alexandria_"));

        // The old dotted treatment and its letterspacing must be gone.
        check("no tatweel in Arabic copy", !AR.toString().contains("\u0640"));
        check("no dotted Arabic class reference", !read(new File(JAVA,
                "com/dotpray/app/widget/WidgetRenderer.java")).contains("ArabicDots"));
        check("no Almarai reference", !javaCode().contains("Almarai"));

        // The design keeps Latin numerals everywhere; resource copy must not
        // smuggle in Arabic-Indic figures that Doto cannot draw.
        File[] arabicFiles = VALUES_AR.listFiles(
                (dir, name) -> name.endsWith(".xml"));
        check("Arabic resource files found",
                arabicFiles != null && arabicFiles.length >= 3);
        boolean indicDigits = false;
        if (arabicFiles != null) {
            for (File file : arabicFiles) {
                String xml = read(file);
                for (int i = 0; i < xml.length(); i++) {
                    char ch = xml.charAt(i);
                    if (ch >= '\u0660' && ch <= '\u0669') {
                        indicDigits = true;
                        break;
                    }
                }
                if (indicDigits) break;
            }
        }
        check("Arabic resources use Latin numerals", !indicDigits);
    }

    private static void arabicPlurals() {
        section("Arabic plurals");
        Map<String, String> full = AR_PLURALS.get("elapsed_minutes");
        Map<String, String> compact = AR_PLURALS.get("elapsed_minutes_compact");
        check("full plural exists", full != null);
        check("compact plural exists", compact != null);
        if (full == null || compact == null) return;

        Set<String> required = new TreeSet<>(Arrays.asList(
                "zero", "one", "two", "few", "many", "other"));
        eq("full quantities", required, new TreeSet<>(full.keySet()));
        eq("compact quantities", required, new TreeSet<>(compact.keySet()));

        for (String quantity : Arrays.asList("zero", "one", "two")) {
            String text = full.get(quantity);
            check(quantity + " uses the prayer name argument",
                    text.contains("%2$s"));
            check(quantity + " does not use the minute count as a string",
                    !text.contains("%1$s"));
            check(quantity + " has no double space", !text.contains("  "));
            check(quantity + " has no tatweel", !text.contains("\u0640"));
        }
        for (String quantity : Arrays.asList("few", "many", "other")) {
            String text = full.get(quantity);
            check(quantity + " includes the minute count", text.contains("%1$d"));
            check(quantity + " includes the prayer name", text.contains("%2$s"));
            check(quantity + " has no double space", !text.contains("  "));
            check(quantity + " has no tatweel", !text.contains("\u0640"));
        }

        // Spot-check the actual Arabic grammar the user sees.
        eq("zero form", "مرّت لحظات على %2$s", full.get("zero"));
        eq("one form", "مرّت دقيقة واحدة على %2$s", full.get("one"));
        eq("two form", "مرّت دقيقتان على %2$s", full.get("two"));
        check("few form uses دقائق", full.get("few").contains("دقائق"));
        check("many form uses دقيقة", full.get("many").contains("دقيقة"));

        for (String quantity : compact.keySet()) {
            String text = compact.get(quantity);
            check("compact " + quantity + " has no tatweel",
                    !text.contains("\u0640"));
            check("compact " + quantity + " has no double space",
                    !text.contains("  "));
        }
        check("compact few uses دقائق", compact.get("few").contains("دقائق"));
    }

    private static void arrays() throws Exception {
        section("resource arrays");
        Map<String, Integer> en = arraySizes("values");
        Map<String, Integer> ar = arraySizes("values-ar");
        eq("array names", en.keySet(), ar.keySet());
        for (String name : en.keySet()) {
            eq("array size " + name, en.get(name), ar.get(name));
        }
        eq("hijri months", 12, en.get("hijri_months"));
        eq("short hijri months", 12, en.get("hijri_months_short"));
        check("Arabic Hijri months are Arabic",
                read(new File(VALUES_AR, "arrays.xml")).contains("رمضان"));
    }

    private static Map<String, Integer> arraySizes(String folder) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File(new File(RES, folder), "arrays.xml"));
        Map<String, Integer> sizes = new LinkedHashMap<>();
        NodeList arrays = doc.getElementsByTagName("string-array");
        for (int i = 0; i < arrays.getLength(); i++) {
            Element array = (Element) arrays.item(i);
            sizes.put(array.getAttribute("name"),
                    array.getElementsByTagName("item").getLength());
        }
        return sizes;
    }

    private static void fonts() {
        section("bundled fonts");
        File regular = new File(RES, "font/alexandria_regular.ttf");
        File semibold = new File(RES, "font/alexandria_semibold.ttf");
        File license = new File("app/src/main/assets/OFL-Alexandria.txt");
        check("Alexandria Regular exists", regular.isFile());
        check("Alexandria SemiBold exists", semibold.isFile());
        check("Alexandria Regular is a real font", regular.length() > 50_000);
        check("Alexandria SemiBold is a real font", semibold.length() > 50_000);
        check("Alexandria OFL exists", license.isFile());
        String text = read(license);
        check("OFL names the license", text.contains("SIL OPEN FONT LICENSE"));
        check("OFL names Alexandria", text.contains("Alexandria"));
        check("Doto Regular retained",
                new File(RES, "font/doto_regular.ttf").isFile());
        check("Doto Bold retained", new File(RES, "font/doto_bold.ttf").isFile());
        check("no runtime font download", !javaCode().contains("downloadFile"));

        File[] almarai = new File(RES, "font").listFiles(
                (dir, name) -> name.toLowerCase(Locale.US).startsWith("almarai"));
        check("Almarai font files removed", almarai == null || almarai.length == 0);
        check("old dotted Arabic source removed", !new File(JAVA,
                "com/dotpray/app/text/ArabicDots.java").exists());
        try {
            check("Alexandria weights are distinct files",
                    !Arrays.equals(Files.readAllBytes(regular.toPath()),
                            Files.readAllBytes(semibold.toPath())));
        } catch (IOException e) {
            check("Alexandria weights are readable", false);
        }
    }

    private static void layouts() {
        section("layouts");
        String main = read(new File(RES, "layout/activity_main.xml"));
        String settings = read(new File(RES, "layout/dialog_settings.xml"));
        String item = read(new File(RES, "layout/item_prayer.xml"));
        check("main has no dotted Arabic view", !main.contains("ArabicDotView"));
        check("item has no dotted Arabic view", !item.contains("ArabicDotView"));
        check("main has no old Arabic string", !main.contains("arabic_"));
        check("item has no old Arabic string", !item.contains("arabic_"));
        check("settings has Arabic language pill",
                settings.contains("btnLanguageArabic"));
        check("settings has English language pill",
                settings.contains("btnLanguageEnglish"));
        check("settings has app language label",
                settings.contains("@string/app_language"));
        check("settings shows the developer credit",
                settings.contains("@string/about_credits"));
        check("settings about is handled by the rounded i row",
                settings.contains("android:id=\"@+id/rowAbout\"")
                        && settings.contains("@drawable/ic_info"));
        check("Arabic language pill keeps Alexandria and tight tracking",
                Pattern.compile("btnLanguageArabic[\\s\\S]{0,500}"
                                + "fontFamily=\"@font/alexandria_semibold\"")
                        .matcher(settings).find()
                        && Pattern.compile("btnLanguageArabic[\\s\\S]{0,500}"
                                + "letterSpacing=\"0\"")
                        .matcher(settings).find());
        check("main numeric hero uses Doto",
                main.contains("id=\"@+id/nextTime\"")
                        && main.contains("style=\"@style/DisplayNumeric\""));
        check("schedule time uses Doto",
                item.contains("id=\"@+id/pTime\"")
                        && item.contains("style=\"@style/DisplayNumeric\""));
        check("brand row is explicitly LTR",
                main.contains("android:layoutDirection=\"ltr\""));
        check("brand wordmark is explicitly start-aligned",
                main.contains("android:text=\"@string/brand_wordmark\"")
                        && main.contains("android:textDirection=\"ltr\""));
        check("action buttons share one fixed height",
                main.contains("android:id=\"@+id/btnAddWidget\"")
                        && main.contains("android:layout_height=\"52dp\"")
                        && main.contains("android:id=\"@+id/btnSettingsGhost\"")
                        && main.contains("android:layout_weight=\"58\"")
                        && main.contains("android:layout_weight=\"42\""));
        check("action labels are single-line",
                count(main, "android:singleLine=\"true\"") >= 2);
        check("location card has explicit edge margins",
                main.contains("android:id=\"@+id/tileLocation\"")
                        && main.contains("android:layout_marginStart=\"0dp\"")
                        && main.contains("android:layout_marginEnd=\"0dp\""));
        // View.LAYOUT_DIRECTION_LOCALE reads Locale.getDefault(), not the
        // in-app language, so forcing it on the card would leave an Arabic
        // screen LTR on an English phone. Inherit the activity direction.
        check("location card inherits the app layout direction",
                main.contains("android:id=\"@+id/tileLocation\"")
                        && !main.contains("android:layoutDirection=\"locale\""));
        check("location card Arabic copy follows the app locale",
                main.contains("android:id=\"@+id/locationHint\"")
                        && main.contains("android:textDirection=\"anyRtl\""));
        check("Latin coordinates keep LTR bidi order",
                main.contains("android:id=\"@+id/locationCoords\"")
                        && main.contains("android:textDirection=\"ltr\""));
        check("mixed location names use first-strong bidi",
                main.contains("android:id=\"@+id/locationValue\"")
                        && main.contains("android:textDirection=\"firstStrong\""));
        String activity = read(new File(JAVA, "com/dotpray/app/MainActivity.java"));
        check("location readouts use the place name without the country",
                activity.contains("PlaceNames.city(this, place.name)")
                        && !activity.contains("PlaceNames.displayName("));
        check("widget chooser layout exists",
                new File(RES, "layout/dialog_widget_picker.xml").isFile());
        check("minimal widget preview layout exists",
                new File(RES, "layout/widget_preview_minimal.xml").isFile());
        check("wide settings option is labelled 4x2",
                EN.get("widget_wide_button").contains("4×2"));
        check("small settings option is labelled 3x3",
                EN.get("widget_small_button").contains("3×3"));
        check("minimal settings option is labelled 2x2",
                EN.get("widget_minimal_button").contains("2×2"));
        check("all three settings options are wired",
                settings.contains("btnWidgetWide")
                        && settings.contains("btnWidgetSmall")
                        && settings.contains("btnWidgetMinimal"));

        // Every inflatable layout, not just the three main screens: a leftover
        // preview label must not ship untranslated.
        File[] layoutFiles = new File(RES, "layout").listFiles(
                (dir, name) -> name.endsWith(".xml"));
        check("layout folder found", layoutFiles != null && layoutFiles.length >= 7);
        if (layoutFiles != null) {
            Arrays.sort(layoutFiles);
            for (File layoutFile : layoutFiles) {
                Set<String> hardcoded = hardcodedText(read(layoutFile));
                check("layout " + layoutFile.getName()
                                + " has no hardcoded android:text/hint/contentDescription: "
                                + hardcoded,
                        hardcoded.isEmpty());
            }
        }

        String arStyles = read(new File(VALUES_AR, "styles.xml"));
        check("Arabic does not override numeric styles",
                !Pattern.compile("<style name=\"(DisplayNumeric|ValueNumeric|LabelNumeric)\"")
                        .matcher(arStyles).find());
        check("Arabic labels use Alexandria", arStyles.contains("@font/alexandria_"));
        check("Arabic Display uses Alexandria",
                arStyles.contains("<style name=\"Display\"")
                        && arStyles.contains("@font/alexandria_semibold"));
    }

    private static void scales() throws Exception {
        section("Arabic/English scale");
        Map<String, String> en = dimens("values");
        Map<String, String> ar = dimens("values-ar");
        check("English text dimensions loaded", en.size() >= 12);
        check("Arabic text dimensions loaded", ar.size() >= 12);
        check("text dimension names match", en.keySet().equals(ar.keySet()));
        for (String key : en.keySet()) {
            float english = sp(en.get(key));
            float arabic = sp(ar.get(key));
            check(key + " is smaller in Arabic", arabic < english);
        }

        String main = read(new File(RES, "layout/activity_main.xml"));
        String item = read(new File(RES, "layout/item_prayer.xml"));
        String settings = read(new File(RES, "layout/dialog_settings.xml"));
        String list = read(new File(RES, "layout/dialog_list.xml"));
        String picker = read(new File(RES, "layout/dialog_widget_picker.xml"));
        check("hero prayer name uses the shared scale",
                main.contains("@dimen/text_prayer_hero"));
        check("hero grace line uses the shared scale",
                main.contains("@dimen/text_grace"));
        check("location value uses the shared scale",
                main.contains("@dimen/text_location"));
        check("date and Hijri lines use the shared scale",
                main.contains("@dimen/text_date_max")
                        && main.contains("@dimen/text_hijri_max"));
        check("schedule prayer name uses the shared scale",
                item.contains("@dimen/text_prayer_row"));
        check("schedule status uses the shared scale",
                item.contains("@dimen/text_status"));
        check("dialog titles use the shared scale",
                settings.contains("@dimen/text_dialog_title")
                        && list.contains("@dimen/text_dialog_title")
                        && picker.contains("@dimen/text_dialog_title"));
        check("settings controls use the shared scale",
                settings.contains("@dimen/text_segment")
                        && settings.contains("@dimen/text_value"));

        String arStyles = read(new File(VALUES_AR, "styles.xml"));
        check("Arabic labels use the shared scale",
                arStyles.contains("@dimen/text_label"));
        check("Arabic values use the shared scale",
                arStyles.contains("@dimen/text_value"));
        check("Arabic buttons use the shared scale",
                arStyles.contains("@dimen/text_button"));

        String renderer = read(new File(JAVA,
                "com/dotpray/app/widget/WidgetRenderer.java"));
        check("widget Arabic labels are scaled down",
                renderer.contains("ARABIC_TEXT_SCALE = 0.8f")
                        && renderer.contains("rtl ? sp * ARABIC_TEXT_SCALE : sp"));
    }

    private static Map<String, String> dimens(String folder) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        File file = new File(new File(RES, folder), "dimens.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(file);
        NodeList list = doc.getElementsByTagName("dimen");
        for (int i = 0; i < list.getLength(); i++) {
            Element dimen = (Element) list.item(i);
            values.put(dimen.getAttribute("name"), dimen.getTextContent().trim());
        }
        return values;
    }

    private static float sp(String value) {
        return Float.parseFloat(value.replace("sp", "").trim());
    }

    private static Set<String> hardcodedText(String layout) {
        Set<String> bad = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile(
                "android:(?:text|hint|contentDescription)=\"([^\"]*)\"").matcher(layout);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (!value.isEmpty() && !value.startsWith("@string/")) {
                bad.add(value);
            }
        }
        return bad;
    }

    private static void widgetMetadata() {
        section("widget metadata");
        // Both the base and the API 31+ override must advertise the same
        // resizable targets. The v31 variant silently wins on Android 12+.
        for (String variant : new String[]{"xml", "xml-v31"}) {
            String wide = read(new File(RES, variant + "/widget_prayer_info.xml"));
            String small = read(new File(RES, variant + "/widget_prayer_small_info.xml"));
            String minimal = read(new File(RES, variant
                    + "/widget_prayer_minimal_info.xml"));
            check(variant + ": all three widgets resize both ways",
                    wide.contains("android:resizeMode=\"horizontal|vertical\"")
                            && small.contains("android:resizeMode=\"horizontal|vertical\"")
                            && minimal.contains("android:resizeMode=\"horizontal|vertical\""));
            check(variant + ": wide widget targets 4x2",
                    wide.contains("android:targetCellWidth=\"4\"")
                            && wide.contains("android:targetCellHeight=\"2\""));
            check(variant + ": small widget targets 3x3",
                    small.contains("android:targetCellWidth=\"3\"")
                            && small.contains("android:targetCellHeight=\"3\""));
            check(variant + ": minimal widget targets 2x2",
                    minimal.contains("android:targetCellWidth=\"2\"")
                            && minimal.contains("android:targetCellHeight=\"2\""));
            check(variant + ": wide widget keeps its 4x2 minimum",
                    wide.contains("android:minWidth=\"250dp\"")
                            && wide.contains("android:minHeight=\"110dp\""));
            check(variant + ": small widget keeps its 3x3 minimum",
                    small.contains("android:minWidth=\"180dp\"")
                            && small.contains("android:minHeight=\"180dp\""));
            check(variant + ": minimal widget keeps its 2x2 minimum",
                    minimal.contains("android:minWidth=\"110dp\"")
                            && minimal.contains("android:minHeight=\"110dp\""));
            check(variant + ": wide resize bounds stay readable",
                    wide.contains("android:minResizeWidth=\"220dp\"")
                            && wide.contains("android:minResizeHeight=\"90dp\""));
            check(variant + ": compact resize bounds stay near 2x2",
                    small.contains("android:minResizeWidth=\"100dp\"")
                            && small.contains("android:minResizeHeight=\"100dp\"")
                            && minimal.contains("android:minResizeWidth=\"100dp\"")
                            && minimal.contains("android:minResizeHeight=\"100dp\""));
            check(variant + ": wide widget uses the wide initial layout",
                    wide.contains("android:initialLayout=\"@layout/widget_prayer\""));
            check(variant + ": compact widgets use the small initial layout",
                    small.contains("android:initialLayout=\"@layout/widget_prayer_small\"")
                            && minimal.contains(
                            "android:initialLayout=\"@layout/widget_prayer_small\""));
            check(variant + ": the providers keep distinct previews",
                    wide.contains("android:previewLayout=\"@layout/widget_preview_wide\"")
                            && small.contains(
                            "android:previewLayout=\"@layout/widget_preview_small\"")
                            && minimal.contains(
                            "android:previewLayout=\"@layout/widget_preview_minimal\""));
            check(variant + ": all three widgets carry descriptions",
                    wide.contains("android:description=\"@string/widget_description\"")
                            && small.contains(
                            "android:description=\"@string/widget_small_description\"")
                            && minimal.contains(
                            "android:description=\"@string/widget_minimal_description\""));
            check(variant + ": preview drawables exist",
                    wide.contains("android:previewImage=\"@drawable/widget_preview\"")
                            && small.contains(
                            "android:previewImage=\"@drawable/widget_preview_small\"")
                            && minimal.contains(
                            "android:previewImage=\"@drawable/widget_preview_minimal\""));
        }
    }

    private static void widgetSettings() {
        section("widget settings");

        // The first configuration intent can arrive before the launcher has
        // bound the id, so every provider needs its own configure screen. Both
        // resource folders must agree: xml-v31 silently wins on Android 12+.
        String[][] variants = {
                {"widget_prayer_info.xml", "WideWidgetConfigActivity", "KIND_WIDE"},
                {"widget_prayer_small_info.xml", "SmallWidgetConfigActivity",
                        "KIND_SMALL"},
                {"widget_prayer_minimal_info.xml", "MinimalWidgetConfigActivity",
                        "KIND_MINIMAL"}
        };
        for (String folder : new String[]{"xml", "xml-v31"}) {
            for (String[] variant : variants) {
                String info = read(new File(RES, folder + "/" + variant[0]));
                check(folder + ": " + variant[1] + " is the configure screen",
                        info.contains("android:configure=\"com.dotpray.app.widget."
                                + variant[1] + "\""));
                check(folder + ": " + variant[1] + " can be reconfigured",
                        info.contains(
                                "android:widgetFeatures=\"reconfigurable\""));
            }
        }

        String manifest = read(new File("app/src/main/AndroidManifest.xml"));
        check("exactly three configuration activities are declared",
                count(manifest, "android.appwidget.action.APPWIDGET_CONFIGURE") == 3);
        for (String[] variant : variants) {
            String block = activityBlock(manifest, variant[1]);
            check(variant[1] + " is exported for the launcher",
                    block.contains("android:exported=\"true\""));
            check(variant[1] + " handles APPWIDGET_CONFIGURE",
                    block.contains(
                            "android.appwidget.action.APPWIDGET_CONFIGURE"));
            String source = read(new File(JAVA,
                    "com/dotpray/app/widget/" + variant[1] + ".java"));
            check(variant[1] + " extends the shared screen",
                    source.contains("extends WidgetConfigActivity"));
            check(variant[1] + " previews its own layout",
                    source.contains("return " + variant[2] + ";"));
        }

        String prefs = read(new File(JAVA,
                "com/dotpray/app/widget/WidgetPrefs.java"));
        check("appearance lives in its own preferences file",
                prefs.contains("dotpray_widget_prefs"));
        check("appearance is keyed per placement",
                prefs.contains("KEY_TRANSPARENCY + appWidgetId"));
        check("transparency is stored as a continuous level",
                prefs.contains("public static int getTransparency")
                        && prefs.contains("public static void setTransparency"));
        check("stored transparency is clamped to 0-100",
                prefs.contains("Math.max(SOLID, Math.min(CLEAR, value))"));
        check("the unreleased binary choice still migrates",
                prefs.contains("LEGACY_BACKGROUND")
                        && prefs.contains("? CLEAR : SOLID"));
        check("invalid placements never write appearance",
                prefs.contains("if (appWidgetId < 0) return;"));
        check("removing a widget removes only its appearance",
                prefs.contains("editor.remove(KEY_TRANSPARENCY + id)"));
        check("restored widgets keep their appearance",
                prefs.contains("public static void restore(Context context"));
        check("restore pairs old and new placements by index",
                prefs.contains("Math.min(oldWidgetIds.length, newWidgetIds.length)")
                        && prefs.contains("int from = oldWidgetIds[i];")
                        && prefs.contains("int to = newWidgetIds[i];"));
        check("restore migrates the binary choice too",
                prefs.contains("LEGACY_BACKGROUND + from, 0) == 1")
                        && prefs.contains("editor.putInt(KEY_TRANSPARENCY + to, CLEAR)"));
        check("restore never overwrites an id with itself",
                prefs.contains("from == to"));

        String config = read(new File(JAVA,
                "com/dotpray/app/widget/WidgetConfigActivity.java"));
        check("configuration reads the placement id",
                config.contains("AppWidgetManager.EXTRA_APPWIDGET_ID"));
        check("configuration starts in the cancelled state",
                config.contains("setResult(RESULT_CANCELED, resultIntent())"));
        check("apply saves only this placement",
                config.contains("WidgetPrefs.setTransparency(this, appWidgetId, selectedTransparency)"));
        check("apply repaints only this placement",
                config.contains("WidgetUpdater.updateOne(this, appWidgetId)"));
        check("apply returns the placement id",
                config.contains("setResult(RESULT_OK, resultIntent())"));
        check("rotation restores the pending level",
                config.contains("STATE_TRANSPARENCY")
                        && config.contains("onSaveInstanceState"));
        check("the screen binds a 0-100 transparency bar",
                config.contains("configTransparencyBar")
                        && config.contains("SeekBar.OnSeekBarChangeListener"));
        check("the live preview follows the slider",
                config.contains("requestPreview")
                        && config.contains("setTransparency(progress)"));
        check("the level has a live percentage readout",
                config.contains("configTransparencyValue")
                        && config.contains("R.string.widget_transparency_value"));
        check("preview asks for the placement size",
                config.contains("WidgetUpdater.previewPixels(this, appWidgetId"));
        check("preview renders all three variants",
                config.contains("WidgetRenderer.wide")
                        && config.contains("WidgetRenderer.small")
                        && config.contains("WidgetRenderer.minimal"));
        check("configuration uses the localized screen title",
                config.contains("setTitle(R.string.widget_settings_title)"));

        String updater = read(new File(JAVA,
                "com/dotpray/app/widget/WidgetUpdater.java"));
        check("every repaint reads the setting for its own id",
                count(updater, "int transparency = WidgetPrefs.getTransparency(context, id);")
                        == 2);
        check("a single placement can be repainted",
                updater.contains(
                        "public static void updateOne(Context context, int appWidgetId)"));
        check("the live preview can use the placement size",
                updater.contains("public static int[] previewPixels"));
        check("the live preview falls back to the design size, not the minimum",
                updater.contains("return pixelsFromDp(context, wDp, hDp);")
                        && !updater.substring(updater.indexOf("previewPixels"),
                        updater.indexOf("private static int[] pixels(")).contains(
                        "getAppWidgetInfo"));
        check("transparency is read through WidgetPrefs",
                updater.contains("WidgetPrefs.getTransparency(context, id)"));
        check("every repaint clears the containers on the host",
                count(updater, "clearSurface(views);") == 2);
        check("both widget containers are reset to transparent",
                updater.contains("R.id.wRoot, \"setBackgroundColor\"")
                        && updater.contains("R.id.wCanvas, \"setBackgroundColor\"")
                        && updater.contains("Color.TRANSPARENT"));
        check("the surface reset explains RemoteViews recycling",
                updater.contains("reapply")
                        && updater.contains("layout attributes"));

        String[] providers = {
                "PrayerWidgetProvider",
                "SmallPrayerWidgetProvider",
                "MinimalPrayerWidgetProvider"
        };
        for (String provider : providers) {
            String source = read(new File(JAVA,
                    "com/dotpray/app/widget/" + provider + ".java"));
            check(provider + " clears settings when removed",
                    source.contains("WidgetPrefs.delete(context, appWidgetIds)"));
            check(provider + " migrates settings when restored",
                    source.contains("onRestored(Context context, int[] oldWidgetIds")
                            && source.contains("WidgetPrefs.restore(context, oldWidgetIds, newWidgetIds)"));
        }

        String renderer = read(new File(JAVA,
                "com/dotpray/app/widget/WidgetRenderer.java"));
        check("widget bitmap keeps a real alpha channel",
                renderer.contains("Bitmap.Config.ARGB_8888"));
        check("card and dot field are skipped only at full clarity",
                count(renderer, "if (transparency < WidgetPrefs.CLEAR) {") == 2
                        && count(renderer, "ink.card();") == 2
                        && count(renderer, "ink.dotField();") == 2);
        check("card fill, border and dot field fade by the saved level",
                renderer.contains("fill(fade(cardColor))")
                        && renderer.contains("stroke.setColor(fade(hairline))")
                        && renderer.contains("fill(fade(dotColor))"));
        check("no other full-canvas fill exists",
                !renderer.contains("drawColor(")
                        && count(renderer, "canvas.drawRoundRect(new RectF(0, 0, W, H)") == 1);
        check("type and times stay opaque at every level",
                !renderer.contains("p.setAlpha(")
                        && !renderer.contains("fg = withAlpha"));
        check("the wallpaper outline scales with transparency",
                renderer.contains("if (transparency > 0)")
                        && renderer.contains("Math.round(0xFF * (transparency / 100f))")
                        && renderer.contains("paint.setStyle(Paint.Style.STROKE)"));
        check("quiet rail dots strengthen over wallpaper",
                renderer.contains("fill(railColor())")
                        && renderer.contains("base + (clear - base) * level"));
        check("outline is dark behind light type and light behind dark type",
                renderer.contains("textOutline = 0xFF000000")
                        && renderer.contains("textOutline = 0xFFFFFFFF"));
        check("clear widgets switch to wallpaper-safe ink",
                renderer.contains("transparency >= 80")
                        && renderer.contains("fg = 0xFFF4F4F2")
                        && renderer.contains("accentText = ACCENT_TEXT_NIGHT"));
        check("clear light widgets use light quiet rail dots",
                renderer.contains("railQuiet = wallpaperInk ? 0x66FFFFFF : 0x66000000"));

        String layout = read(new File(RES, "layout/activity_widget_config.xml"));
        for (String id : new String[]{"configScroll", "configContent",
                "configPreviewStage", "configPreviewCanvas", "configPreviewMode",
                "configVariantTag", "configTransparencyBar",
                "configTransparencyValue", "btnConfigBack", "btnConfigCancel",
                "btnConfigApply"}) {
            check("configuration layout binds " + id,
                    layout.contains("@+id/" + id));
        }
        check("configuration layout has no hardcoded copy",
                hardcodedText(layout).isEmpty());
        check("configuration exposes the full transparency range",
                layout.contains("android:max=\"100\"")
                        && layout.contains("android:progress=\"0\""));
        check("configuration labels both ends of the bar",
                layout.contains("@string/widget_transparency_solid")
                        && layout.contains("@string/widget_transparency_clear"));
        check("configuration explains the transparency range",
                layout.contains("@string/widget_transparency_desc"));
        check("configuration preview sits on a wallpaper sample",
                layout.contains("@drawable/bg_widget_stage")
                        && layout.contains("DotFieldView"));
        check("the square preview cannot push the controls off screen",
                layout.contains("android:maxHeight=\"190dp\""));
        check("configuration has explicit Cancel and Apply actions",
                layout.contains("@string/widget_settings_cancel")
                        && layout.contains("@string/widget_settings_apply"));
        check("preview stage has a light sample",
                new File(RES, "drawable/bg_widget_stage.xml").isFile());
        check("preview stage has a night sample",
                new File(RES, "drawable-night/bg_widget_stage.xml").isFile());

        String[] copy = {"widget_settings_title", "widget_settings_subtitle",
                "widget_settings_preview", "widget_settings_preview_a11y",
                "widget_settings_preview_mode", "widget_settings_transparency",
                "widget_transparency_solid", "widget_transparency_clear",
                "widget_transparency_value", "widget_transparency_desc",
                "widget_transparency_a11y", "widget_settings_note",
                "widget_settings_apply", "widget_settings_cancel"};
        for (String key : copy) {
            check("widget settings copy exists: " + key, EN.containsKey(key));
            if ("widget_transparency_value".equals(key)) {
                // A bare figure uses Doto's Latin digits and percent sign in
                // both languages; there is nothing to translate.
                check("percentage literal is language-neutral",
                        AR.containsKey(key) && EN.get(key).equals(AR.get(key))
                                && EN.get(key).contains("%1$d"));
            } else {
                check("widget settings copy is translated: " + key,
                        AR.containsKey(key) && !EN.get(key).equals(AR.get(key)));
            }
        }
        check("the screen is titled Widget Settings",
                "Widget Settings".equals(EN.get("widget_settings_title")));
        check("Arabic widget settings title is Arabic",
                hasArabic(AR.get("widget_settings_title")));
        check("transparency copy names the wallpaper",
                EN.get("widget_transparency_desc").contains("wallpaper"));
        check("Arabic transparency copy names the wallpaper",
                AR.get("widget_transparency_desc").contains("خلفية الشاشة"));
        check("transparency copy names both endpoints",
                EN.get("widget_transparency_desc").contains("0%")
                        && EN.get("widget_transparency_desc").contains("100%"));
        check("the note keeps type and times opaque",
                EN.get("widget_settings_note").contains("opaque"));
        check("the note applies only to this placement",
                EN.get("widget_settings_note").contains("this widget"));
        check("Arabic note stays Arabic",
                hasArabic(AR.get("widget_settings_note")));

        for (String name : new String[]{"widget_prayer.xml",
                "widget_prayer_small.xml"}) {
            String widget = read(new File(RES, "layout/" + name));
            check(name + " has no placeholder image",
                    !widget.contains("android:src="));
            check(name + " is transparent down to the canvas",
                    count(widget, "android:background=\"@color/transparent\"") == 2);
        }
    }

    /**
     * Returns one activity element from the manifest, or an empty string when
     * the activity is missing.
     */
    private static String activityBlock(String manifest, String className) {
        int start = manifest.indexOf("android:name=\".widget." + className + "\"");
        if (start < 0) return "";
        int end = manifest.indexOf("</activity>", start);
        return end < 0 ? manifest.substring(start) : manifest.substring(start, end);
    }

    private static boolean hasArabic(String text) {
        return text != null && ARABIC.matcher(text).find();
    }

    private static void javaCopy() {
        section("Java copy");
        String all = javaCode();
        check("no hardcoded Arabic elapsed sentence",
                !all.contains("\"مرّت"));
        check("no hardcoded SINCE line", !all.contains("\"SINCE "));
        check("no hardcoded NEXT PRAYER", !all.contains("\"NEXT PRAYER\""));
        check("no hardcoded PRAYER NOW", !all.contains("\"PRAYER NOW\""));
        check("no hardcoded minute-since wording",
                !all.contains(" minutes since the Adhan"));
        check("no old dotted renderer", !all.contains("ArabicDots"));
        check("no old Arabic text helper", !all.contains("ArabicText"));
        check("no old Arabic view", !all.contains("ArabicDotView"));
        check("language selector callback exists",
                all.contains("onLanguageChanged"));
        check("widget renderer resolves prayer names from resources",
                all.contains("PrayerNames.display"));
        check("elapsed wording resolves from plurals",
                all.contains("R.plurals.elapsed_minutes"));
        check("widget accessibility avoids repeating the prayer name",
                all.contains("ElapsedText.compact(context, model.elapsedMinutes)"));
        check("numeric resource formatting forces Latin digits",
                all.contains("String.format(Locale.US, context.getString(id), args)")
                        && all.contains("String.format(Locale.US, raw, args)"));
        check("elapsed text uses the Latin-digit plural helper",
                all.contains("UiText.quantity(context, R.plurals.elapsed_minutes"));
        check("widget countdown uses the Latin-digit formatter",
                all.contains("UiText.format(context, R.string.duration_hours_minutes"));
        check("Add Widget opens a three-layout chooser",
                all.contains("showWidgetSizes")
                        && all.contains("WidgetSizeDialog.Kind"));
        check("all three providers remain wired to the chooser",
                all.contains("PrayerWidgetProvider.class")
                        && all.contains("SmallPrayerWidgetProvider.class")
                        && all.contains("MinimalPrayerWidgetProvider.class"));
        check("minimal widget omits the progress rail",
                all.contains("WidgetRenderer.minimal")
                        && all.contains("if (rail)"));
        check("resize events refresh every provider",
                all.contains("onAppWidgetOptionsChanged")
                        && all.contains("updateMinimal"));
        check("progress geometry is shared with the widgets",
                all.contains("ProgressDots.completed")
                        && all.contains("ProgressDots.isComplete"));
    }

    private static void manifest() throws Exception {
        section("manifest");
        String manifest = read(new File("app/src/main/AndroidManifest.xml"));
        check("supportsRtl is enabled", manifest.contains("android:supportsRtl=\"true\""));
        check("no INTERNET permission", !manifest.contains("android.permission.INTERNET"));
        check("app id unchanged", manifest.contains("package=\"com.dotpray.app\""));
        check("wide widget has a localized picker label",
                manifest.contains("android:label=\"@string/widget_label\""));
        check("small widget has a localized picker label",
                manifest.contains("android:label=\"@string/widget_small_label\""));
        check("minimal widget has a localized picker label",
                manifest.contains("android:label=\"@string/widget_minimal_label\""));
        check("English widget labels are distinct",
                !EN.get("widget_label").equals(EN.get("widget_small_label"))
                        && !EN.get("widget_small_label").equals(
                        EN.get("widget_minimal_label"))
                        && !EN.get("widget_label").equals(
                        EN.get("widget_minimal_label")));
        check("Arabic widget labels are distinct",
                !AR.get("widget_label").equals(AR.get("widget_small_label"))
                        && !AR.get("widget_small_label").equals(
                        AR.get("widget_minimal_label"))
                        && !AR.get("widget_label").equals(
                        AR.get("widget_minimal_label")));
        check("widget picker label is translated",
                !EN.get("widget_label").equals(AR.get("widget_label")));
        check("small widget picker label is translated",
                !EN.get("widget_small_label").equals(AR.get("widget_small_label")));
        check("minimal widget picker label is translated",
                !EN.get("widget_minimal_label").equals(
                        AR.get("widget_minimal_label")));
    }

    private static void rtl() {
        section("RTL");
        String progress = read(new File(JAVA,
                "com/dotpray/app/ui/ProgressLineView.java"));
        check("app progress rail mirrors in RTL",
                progress.contains("LAYOUT_DIRECTION_RTL"));
        check("app progress rail is dotted",
                progress.contains("ProgressDots") && progress.contains("drawCircle")
                        && !progress.contains("drawLine"));
        String theme = read(new File(JAVA,
                "com/dotpray/app/data/ThemeController.java"));
        check("locale applies layout direction",
                theme.contains("setLayoutDirection"));
        String renderer = read(new File(JAVA,
                "com/dotpray/app/widget/WidgetRenderer.java"));
        check("widget uses shared dot geometry",
                renderer.contains("ProgressDots.center")
                        && renderer.contains("ProgressDots.completed"));
        check("widget progress rail is dotted",
                renderer.contains("drawCircle(cx") && !renderer.contains("drawRect(left, y"));
        check("Arabic widget uses Alexandria", renderer.contains("R.font.alexandria_"));
        check("widget has RTL branch", renderer.contains("ink.rtl"));
        String language = read(new File(JAVA,
                "com/dotpray/app/data/Language.java"));
        check("Arabic is RTL", language.contains("return isArabic(language)"));
        check("English is the default", language.contains("DEFAULT = ENGLISH"));
    }

    // -------------------------------------------------------------- helpers

    private static Set<String> placeholders(String text) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text == null ? "" : text);
        while (matcher.find()) found.add(matcher.group(1));
        return found;
    }

    private static int count(String text, String needle) {
        int found = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            found++;
        }
        return found;
    }

    private static String javaText() {
        StringBuilder sb = new StringBuilder();
        collect(new File(JAVA, "com/dotpray/app"), sb);
        return sb.toString();
    }

    private static String javaCode() {
        return stripComments(javaText());
    }

    /**
     * Removes Java comments but keeps string and character literals intact, so
     * a Javadoc example cannot pass for (or fail) a hardcoded-copy check.
     */
    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean string = false;
        boolean character = false;
        boolean line = false;
        boolean block = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (line) {
                if (c == '\n') {
                    line = false;
                    out.append(c);
                }
                continue;
            }
            if (block) {
                if (c == '*' && next == '/') {
                    block = false;
                    i++;
                } else if (c == '\n') {
                    out.append(c);
                }
                continue;
            }
            if (string || character) {
                out.append(c);
                if (c == '\\' && i + 1 < source.length()) {
                    out.append(source.charAt(++i));
                } else if (string && c == '"') {
                    string = false;
                } else if (character && c == '\'') {
                    character = false;
                }
                continue;
            }
            if (c == '/' && next == '/') {
                line = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                block = true;
                i++;
                continue;
            }
            if (c == '"') {
                string = true;
            } else if (c == '\'') {
                character = true;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static void collect(File dir, StringBuilder sb) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collect(file, sb);
            } else if (file.getName().endsWith(".java")) {
                sb.append(read(file)).append('\n');
            }
        }
    }

    private static String read(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static void containsArabic(String what, String text) {
        check(what + " contains Arabic", text != null && ARABIC.matcher(text).find());
    }

    private static void notEqual(String what, String a, String b) {
        check(what, a != null && b != null && !a.equals(b));
    }

    private static void section(String name) {
        System.out.println();
        System.out.println("-- " + name);
    }

    private static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        checks++;
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what + " expected <" + expected
                    + "> but was <" + actual + ">");
        }
    }
}
