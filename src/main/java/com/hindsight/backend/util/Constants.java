package com.hindsight.backend.util;

import java.util.List;

public class Constants {

    public static final List<String> HINT_TRIGGERS = List.of(
        "hint", "help me", "i give up", "just tell me",
        "i don't know", "i dont know", "stuck", "lost",
        "solution", "answer", "tell me how"
    );

    public static final String SOCRATIC_SYSTEM_PROMPT = """
        You are a Socratic coding mentor. Your entire purpose is to help
        students learn to solve problems themselves. You have a strict set
        of rules you never break.
        
        CORE RULES:
        1. Never write code for the student. Not even a single line.
        2. Never give away the answer directly.
        3. Always respond with a guiding question before giving any information.
        4. If a student asks for a hint AND hasn't passed the hint gate yet, tell them
           you will ask them two quick questions first. Do not give the hint yet.
        5. If they HAVE passed the gate (you will be told), give a small, direct
           directional nudge. Do not give the solution.
        6. Keep your tone patient, warm, and encouraging. Never condescending.
        7. If the student is completely wrong, gently correct them and
           ask a follow-up — do not lecture.
        
        WHEN THE STUDENT IS STUCK:
        - Ask one focused question that points them toward the issue.
        - Example: instead of "you need a hash map", ask
          "what data structure lets you look up a value in constant time?"
        
        WHEN GRADING HINT GATE ANSWERS:
        - Accept answers that show genuine understanding, even if the
          exact wording differs.
        - If wrong, correct gently and try once more.
        
        WHEN GIVING AN EARNED HINT:
        - Give one small directional nudge. Not a solution.
        - Example: "Think about what happens to your index when
          the array is empty — does your loop account for that?"
        
        NEVER:
        - Write or complete code for the student
        - Give the full algorithm or approach directly
        - Skip the questioning phase even if the student begs
        
        {memory_context}
        """;

    public static String getHintGatePrompt(String problemTitle, String topic) {
        return String.format("""
            A student is working on the following problem:
            %s
            
            The topic is: %s
            
            They are asking for a hint. Generate exactly 2 simple conceptual
            questions that test whether they understand the core idea behind
            this problem.
            
            The questions must:
            - Be answerable in 1-2 sentences
            - Test understanding, not syntax
            - Be specific to the topic: %s
            
            Respond in this exact JSON format:
            {
              "questions": [
                "question one here",
                "question two here"
              ]
            }
            Return only the JSON. No explanation, no markdown.
            """, problemTitle, topic, topic);
    }

    public static String getGradeAnswerPrompt(String question, String answer, String acceptableConcepts) {
        return String.format("""
            A student answered a concept question about coding.
            
            Question: %s
            Student answer: %s
            Acceptable concepts to look for: %s
            
            Decide if the student demonstrated understanding.
            Be lenient — accept paraphrasing and informal language.
            Only reject if the answer shows clear misunderstanding or is
            completely off-topic.
            
            Respond in this exact JSON format:
            {
              "passed": true or false,
              "feedback": "one sentence of encouraging feedback or gentle correction"
            }
            Return only the JSON. No explanation, no markdown.
            """, question, answer, acceptableConcepts);
    }

    public static String getDirectHintPrompt(String problemTitle, String topic, List<String> previousHints) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("A student is working on the problem: '%s' in the topic of '%s'.\n\n", problemTitle, topic));
        sb.append("They have already proven they understand the core concepts.\n");
        sb.append("Give them a small, directional nudge to help them move forward.\n\n");

        if (previousHints != null && !previousHints.isEmpty()) {
            sb.append("IMPORTANT: The following hints have already been given to the student:\n");
            for (String h : previousHints) {
                sb.append("- ").append(h).append("\n");
            }
            sb.append("\nYour task is to provide a NEW, DIFFERENT hint that focuses on a different aspect of the problem or approach.\n");
        } else {
            sb.append("Give them a fresh starting hint.\n");
        }

        sb.append("\nDo not provide a full solution or code.\n");
        sb.append("One or two sentences max.\n");
        return sb.toString();
    }

    public static String getMistakePatternPrompt(List<String> mistakes) {
        StringBuilder sb = new StringBuilder();
        sb.append("A student made the following mistakes while coding:\n");
        for (int i = 0; i < mistakes.size(); i++) {
            sb.append(i + 1).append(". ").append(mistakes.get(i)).append("\n");
        }
        sb.append("\nIdentify the top recurring patterns in these mistakes.\n");
        sb.append("Respond in this exact JSON format:\n");
        sb.append("{\n");
        sb.append("  \"patterns\": [\n");
        sb.append("    { \"name\": \"pattern name\", \"severity\": \"high|medium|low\", \"description\": \"brief explanation\" }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("Return only the JSON. No explanation, no markdown.\n");
        return sb.toString();
    }
}
