import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizLeaderboardSystem {

    // Your Roll Number
    static String regNo = "RA2311003010093";

    // Base API URL
    static String baseUrl = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";

    public static void main(String[] args) {

        try {
            HttpClient client = HttpClient.newHttpClient();

            // To avoid duplicate entries
            Set<String> uniqueRecords = new HashSet<>();

            // To store total score of each participant
            Map<String, Integer> scores = new HashMap<>();

            // Poll exactly 10 times (0 to 9)
            for (int poll = 0; poll < 10; poll++) {

                String url = baseUrl + "/quiz/messages?regNo=" + regNo + "&poll=" + poll;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                // Convert response to events
                List<Map<String, String>> events = parseEvents(response.body());

                // Read all events
                for (Map<String, String> event : events) {

                    String roundId = event.get("roundId");
                    String participant = event.get("participant");
                    int score = Integer.parseInt(event.get("score"));

                    // Dedup key = roundId + participant
                    String key = roundId + "_" + participant;

                    // Add only if not duplicate
                    if (!uniqueRecords.contains(key)) {
                        uniqueRecords.add(key);

                        scores.put(participant,
                                scores.getOrDefault(participant, 0) + score);
                    }
                }

                // Mandatory 5 second delay after every request
                if (poll < 9) {
                    Thread.sleep(5000);
                }
            }

            // Sort leaderboard by totalScore descending
            List<Map.Entry<String, Integer>> list =
                    new ArrayList<>(scores.entrySet());

            list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            // Prepare leaderboard JSON
            StringBuilder leaderboard = new StringBuilder();
            leaderboard.append("[");

            int totalScore = 0;

            for (int i = 0; i < list.size(); i++) {
                Map.Entry<String, Integer> entry = list.get(i);

                if (i > 0) {
                    leaderboard.append(",");
                }

                leaderboard.append("{");
                leaderboard.append("\"participant\":\"").append(escapeJson(entry.getKey())).append("\",");
                leaderboard.append("\"totalScore\":").append(entry.getValue());
                leaderboard.append("}");

                totalScore += entry.getValue();
            }

            leaderboard.append("]");

            // Print Final Leaderboard
            System.out.println("Leaderboard:");
            System.out.println(leaderboard.toString());

            System.out.println("Total Score = " + totalScore);

            // Submit once
            String submitBody = "{\"regNo\":\"" + escapeJson(regNo) + "\",\"leaderboard\":" + leaderboard + "}";

            HttpRequest postRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/quiz/submit"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(submitBody))
                    .build();

            HttpResponse<String> submitResponse =
                    client.send(postRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("Submission Response:");
            System.out.println(submitResponse.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Map<String, String>> parseEvents(String body) {
        List<Map<String, String>> events = new ArrayList<>();

        int eventsKeyIndex = body.indexOf("\"events\"");
        if (eventsKeyIndex < 0) {
            return events;
        }

        int arrayStart = body.indexOf('[', eventsKeyIndex);
        if (arrayStart < 0) {
            return events;
        }

        int depth = 0;
        int arrayEnd = -1;

        for (int i = arrayStart; i < body.length(); i++) {
            char c = body.charAt(i);

            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    arrayEnd = i;
                    break;
                }
            }
        }

        if (arrayEnd < 0) {
            return events;
        }

        String arrayContent = body.substring(arrayStart + 1, arrayEnd);
        Pattern objectPattern = Pattern.compile("\\{([^{}]*)\\}");
        Matcher objectMatcher = objectPattern.matcher(arrayContent);

        Pattern roundPattern = Pattern.compile("\"roundId\"\\s*:\\s*\"([^\"]*)\"");
        Pattern participantPattern = Pattern.compile("\"participant\"\\s*:\\s*\"([^\"]*)\"");
        Pattern scorePattern = Pattern.compile("\"score\"\\s*:\\s*(-?\\d+)");

        while (objectMatcher.find()) {
            String object = objectMatcher.group(1);

            Matcher roundMatcher = roundPattern.matcher(object);
            Matcher participantMatcher = participantPattern.matcher(object);
            Matcher scoreMatcher = scorePattern.matcher(object);

            if (roundMatcher.find() && participantMatcher.find() && scoreMatcher.find()) {
                Map<String, String> event = new HashMap<>();
                event.put("roundId", roundMatcher.group(1));
                event.put("participant", participantMatcher.group(1));
                event.put("score", scoreMatcher.group(1));
                events.add(event);
            }
        }

        return events;
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '\\') {
                escaped.append("\\\\");
            } else if (c == '\"') {
                escaped.append("\\\"");
            } else if (c == '\b') {
                escaped.append("\\b");
            } else if (c == '\f') {
                escaped.append("\\f");
            } else if (c == '\n') {
                escaped.append("\\n");
            } else if (c == '\r') {
                escaped.append("\\r");
            } else if (c == '\t') {
                escaped.append("\\t");
            } else {
                escaped.append(c);
            }
        }

        return escaped.toString();
    }
}