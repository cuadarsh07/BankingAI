package in.ashish.combined.service.fraud;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for detecting Social Engineering and Digital Arrest scam patterns.
 * Supports English, Hindi, and Hinglish.
 */
@Slf4j
@Service
public class ScamAnalysisService {

    @Data
    @Builder
    public static class ScamScanResult {
        private boolean scamDetected;
        private List<String> categories;
        private List<String> matchedIndicators;
        private double confidence;
        private String explanation;
        private String customerWarning;
        private List<String> contextualQuestions;
    }

    // Pattern groups for scam categories across English, Hindi, and Hinglish
    private static final Map<String, List<Pattern>> SCAM_PATTERNS = new LinkedHashMap<>();

    static {
        // 1. Digital Arrest / Law Enforcement Intimidation
        SCAM_PATTERNS.put("DIGITAL_ARREST", Arrays.asList(
                Pattern.compile("(?iu)(digital\\s*arrest|video\\s*call\\s*arrest|skype\\s*arrest|online\\s*arrest)"),
                Pattern.compile("(?iu)(डिजिटल\\s*अरेस्ट|वीडियो\\s*कॉल|सीबीआई|पुलिस|थाना|गिरफ्तार|वारंट|जेल|हवालात)"),
                Pattern.compile("(?iu)(camera\\s*off\\s*mat\\s*karna|video\\s*call\\s*par\\s*raho|stay\\s*on\\s*camera)"),
                Pattern.compile("(?iu)(cbi\\s*officer|ed\\s*officer|police\\s*officer|cyber\\s*crime|delhi\\s*police|mumbai\\s*police)"),
                Pattern.compile("(?iu)(arrest\\s*warrant|fir\\s*registered|non\\s*bailable|court\\s*order|customs\\s*seizure)"),
                Pattern.compile("(?iu)(police\\s*ne\\s*bola|cbi\\s*ne\\s*bola|thana|police\\s*threatened|hiraasat)"),
                Pattern.compile("(?iu)(mere\\s*against\\s*case|case\\s*ho\\s*gaya|jail\\s*bhej|arrest\\s*kar\\s*lenge)")
        ));

        // 2. Safe Account / Fund Verification Deceit
        SCAM_PATTERNS.put("SAFE_ACCOUNT_SCAM", Arrays.asList(
                Pattern.compile("(?iu)(safe\\s*account|suraksha\\s*khata|verification\\s*account|clearing\\s*account)"),
                Pattern.compile("(?iu)(सुरक्षित\\s*खाता|वेरिफिकेशन\\s*खाता|सरकारी\\s*खाता)"),
                Pattern.compile("(?iu)(rbi\\s*account|government\\s*account|sarkari\\s*khata|police\\s*safe\\s*account)"),
                Pattern.compile("(?iu)(paise\\s*safe\\s*hai|money\\s*will\\s*be\\s*refunded|fund\\s*verification)"),
                Pattern.compile("(?iu)(verify\\s*funds|unfreeze\\s*account|khata\\s*unfreeze)")
        ));

        // 3. Urgency and Pressure
        SCAM_PATTERNS.put("URGENCY_PRESSURE", Arrays.asList(
                Pattern.compile("(?iu)(immediately|urgent|right\\s*now|within\\s*\\d+\\s*(minutes|hours|mins))"),
                Pattern.compile("(?iu)(तुरंत|जल्दी|अभी\\s*के\\s*अभी)"),
                Pattern.compile("(?iu)(jaldi\\s*karo|turant|abhi\\s*ke\\s*abhi|time\\s*nahi\\s*hai)"),
                Pattern.compile("(?iu)(transfer\\s*immediately|pay\\s*within\\s*\\d+\\s*min)")
        ));

        // 4. Secrecy and Isolation Coercion
        SCAM_PATTERNS.put("SECRECY_COERCION", Arrays.asList(
                Pattern.compile("(?iu)(don'?t\\s*tell\\s*anyone|keep\\s*(it\\s*)?secret|confidential\\s*investigation)"),
                Pattern.compile("(?iu)(किसी\\s*को\\s*मत\\s*बताना|गोपनीय)"),
                Pattern.compile("(?iu)(kisi\\s*ko\\s*mat\\s*batana|ghar\\s*walo\\s*ko\\s*mat\\s*bolo|secret\\s*rakho)"),
                Pattern.compile("(?iu)(do\\s*not\\s*disconnect|call\\s*mat\\s*katna|don'?t\\s*hang\\s*up)")
        ));

        // 5. Remote Access / Credential Harvesting
        SCAM_PATTERNS.put("CREDENTIAL_HARVESTING", Arrays.asList(
                Pattern.compile("(?i)(anydesk|teamviewer|rustdesk|screen\\s*share|quicksupport)"),
                Pattern.compile("(?i)(share\\s*otp|pin\\s*batao|password\\s*share|cvv)"),
                Pattern.compile("(?i)(apk\\s*download|custom\\s*app\\s*install|link\\s*pe\\s*click)")
        ));

        // 6. Fake Investment / Work From Home / Task Scam
        SCAM_PATTERNS.put("INVESTMENT_TASK_SCAM", Arrays.asList(
                Pattern.compile("(?i)(telegram\\s*task|youtube\\s*like|guaranteed\\s*profit|double\\s*money)"),
                Pattern.compile("(?i)(crypto\\s*trading\\s*profit|forex\\s*scheme|daily\\s*return\\s*\\d+%)"),
                Pattern.compile("(?i)(paisa\\s*double|daily\\s*income|task\\s*completion)")
        ));
    }

    /**
     * Fast deterministic pattern scan of text (in English, Hindi, or Hinglish).
     */
    public ScamScanResult analyzeText(String... texts) {
        StringBuilder combined = new StringBuilder();
        if (texts != null) {
            for (String t : texts) {
                if (t != null && !t.trim().isEmpty()) {
                    combined.append(" ").append(t.trim());
                }
            }
        }
        String textToAnalyze = combined.toString().trim();

        Set<String> matchedCategories = new LinkedHashSet<>();
        List<String> matchedIndicators = new ArrayList<>();

        if (!textToAnalyze.isEmpty()) {
            for (Map.Entry<String, List<Pattern>> entry : SCAM_PATTERNS.entrySet()) {
                String category = entry.getKey();
                for (Pattern p : entry.getValue()) {
                    var matcher = p.matcher(textToAnalyze);
                    if (matcher.find()) {
                        matchedCategories.add(category);
                        matchedIndicators.add("Matched trigger: '" + matcher.group() + "' under " + category);
                    }
                }
            }
        }

        boolean detected = !matchedCategories.isEmpty();
        double confidence = detected ? Math.min(0.95, 0.65 + (matchedCategories.size() * 0.10)) : 0.05;

        String explanation;
        String customerWarning;

        if (detected) {
            explanation = String.format("High-risk scam indicators detected across %d categories: %s.",
                    matchedCategories.size(), String.join(", ", matchedCategories));

            if (matchedCategories.contains("DIGITAL_ARREST")) {
                customerWarning = "चेतावनी (WARNING): पुलिस, CBI या कोई सरकारी एजेंसी कभी वीडियो कॉल पर पैसे ट्रांसफर करने या डिजिटल अरेस्ट का आदेश नहीं देती। (Government authorities NEVER demand money transfers or order digital arrest over video calls).";
            } else if (matchedCategories.contains("SAFE_ACCOUNT_SCAM")) {
                customerWarning = "चेतावनी (WARNING): कोई भी सुरक्षित या वेरिफिकेशन सरकारी खाता नहीं होता। यह ठगी का प्रयास हो सकता है। (There is no such thing as a 'Safe Government Account'. This is a verified scam tactic).";
            } else {
                customerWarning = "चेतावनी (WARNING): इस लेनदेन में संदिग्ध सामाजिक दबाव या धोखाधड़ी के संकेत मिले हैं। कृपया किसी के बहकावे में आकर पैसे न भेजें।";
            }
        } else {
            explanation = "No explicit social engineering or digital arrest keywords detected in transfer narrative.";
            customerWarning = "Standard banking security checks apply.";
        }

        List<String> questions = generateContextualQuestions(matchedCategories);

        return ScamScanResult.builder()
                .scamDetected(detected)
                .categories(new ArrayList<>(matchedCategories))
                .matchedIndicators(matchedIndicators)
                .confidence(confidence)
                .explanation(explanation)
                .customerWarning(customerWarning)
                .contextualQuestions(questions)
                .build();
    }

    /**
     * Generates targeted security questions based on detected scam categories.
     */
    public List<String> generateContextualQuestions(Collection<String> categories) {
        List<String> questions = new ArrayList<>();

        if (categories != null && categories.contains("DIGITAL_ARREST")) {
            questions.add("Did anyone claiming to be from Police, CBI, ED, Court, or Telecom Department contact you and ask you to transfer funds?");
            questions.add("Are you currently on a phone call or Skype/WhatsApp video call where you were told not to disconnect?");
        }

        if (categories != null && categories.contains("SAFE_ACCOUNT_SCAM")) {
            questions.add("Were you told that this money is being transferred to a 'safe verification account' or 'RBI safe account'?");
        }

        if (categories != null && categories.contains("SECRECY_COERCION")) {
            questions.add("Were you instructed to keep this transaction secret or not tell your family members?");
        }

        // Always include foundational questions
        if (questions.size() < 3) {
            questions.add("Do you personally know and have physically met the recipient of this transfer?");
            questions.add("Are you being pressured to make this transfer immediately under threat of arrest or account blocking?");
            questions.add("Did anyone ask you to install apps like AnyDesk, TeamViewer, or share your screen/OTP?");
        }

        return questions;
    }
}
