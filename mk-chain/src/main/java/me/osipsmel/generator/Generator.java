package me.osipsmel.generator;

import java.util.regex.Pattern;
import java.util.*;
import java.util.stream.Collectors;

import me.osipsmel.api.GeneratorAPI;

public class Generator implements GeneratorAPI{
    public Generator() {}

    private final Random random = new Random();
    private final String gen_delim = " ";

    public String generateBad(String source, String start, int lenght, int depth) {
        List<String> corpus = Tokenizer.tokenize(source);

        Map<String, Map<String, Integer>> transitMap = collectTransitions(corpus, depth);
        List<String> chain = new ArrayList<>();
        if(start != null && !start.isEmpty()){
            chain.addAll(Tokenizer.tokenize(start));
        }
        else{
            List<String> keys = new ArrayList<>(transitMap.keySet());
            String randomKey = keys.get(random.nextInt(keys.size()));
            chain.add(randomKey);       //TODO!
        }

        List<String> generatedTokens = new ArrayList<>();
        for (int i = 0; i < lenght; i++) {
            String nextToken = predictNext(chain, transitMap, depth);
            if (nextToken.isEmpty()) {
                if (!chain.isEmpty()) chain.remove(chain.size() - 1);
                continue;
            }
            generatedTokens.add(nextToken);
            chain.add(nextToken);
        }

        return Tokenizer.textify(generatedTokens);
    }

    @Override
    public Iterator<String> generate(String source, String start, int length, int depth) {
        List<String> corpus = Tokenizer.tokenize(source);
        Map<String, Map<String, Integer>> transitMap = collectTransitions(corpus, depth);
        
        List<String> chain = new ArrayList<>();
        if (start != null && !start.isEmpty()) {
            chain.addAll(Tokenizer.tokenize(start));
        } else {
            chain.add(new ArrayList<>(transitMap.keySet()).get(random.nextInt(transitMap.size())));
        }

    return new Iterator<>() {
        private int count = 0;
        private boolean lastWasNewline = true; // Чтобы не ставить пробел в самом начале

        @Override
        public boolean hasNext() {
            return count < length;
        }

        @Override
        public String next() {
            String nextToken = predictNext(chain, transitMap, depth);
            
            if (nextToken.isEmpty()) {
                if (!chain.isEmpty()) chain.remove(chain.size() - 1);
                return ""; 
            }

            chain.add(nextToken);
            count++;


            String result;
            if (nextToken.equals(Tokenizer.NEWLINE_PLACEHOLDER)) {
                result = Tokenizer.PARAGRAPH_CHARACTER;
                lastWasNewline = true;
            } else if (!lastWasNewline && isWord(nextToken)) {
                result = " " + nextToken; 
                lastWasNewline = false;
            } else {
                result = nextToken;
                lastWasNewline = false;
            }

            return result;
        }
        
        private boolean isWord(String token) {
            return token.matches("[a-zA-Zа-яА-ЯёЁ0-9«\"\\(].*");
        }
    };
}


    private Map<String, Map<String, Integer>> collectTransitions(List<String> corpus, int depth) {
        Map<String, Map<String, Integer>> transitions = new HashMap<>();

        for (int i = 0; i <= corpus.size() - depth; i++) {
            List<String> sample = corpus.subList(i, i + depth);
            
            // state - curent state
            String state = String.join(gen_delim, sample.subList(0, depth-1));
            String nextWord = sample.get(depth-1);

            // collecting transitions matrix
            transitions.computeIfAbsent(state, k -> new HashMap<>())
                       .merge(nextWord, 1, Integer::sum); 
        }
        return transitions;
    }
    private String predictNext(List<String> chain, Map<String, Map<String, Integer>> transitions, int depth) {
        int startIdx = Math.max(0, chain.size() - (depth - 1));
        String lastState = String.join(gen_delim, chain.subList(startIdx, chain.size()));

        Map<String, Integer> nextWordsCounter = transitions.get(lastState);
        if (nextWordsCounter == null || nextWordsCounter.isEmpty()) {
            List<String> keys = new ArrayList<>(transitions.keySet());
            String randomKey = keys.get(random.nextInt(keys.size()));
            String[] parts = randomKey.split(" ");
            return parts[parts.length - 1]; 
        }

        return getRandomWeighted(nextWordsCounter);
    }
    private String getRandomWeighted(Map<String, Integer> weights) {
        int totalWeight = weights.values().stream().reduce(0, (x, y) -> x + y);
        int r = random.nextInt(totalWeight);
        int current = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            current += entry.getValue();
            if (r < current) return entry.getKey();
        }
        return " ";
    }


    private static class Tokenizer{
        private static final String NEWLINE_PLACEHOLDER = "§";
        private static final String PARAGRAPH_CHARACTER = "\n\n";

        private static final Pattern NEWLINES_RE = Pattern.compile("\\n\\s*");
        
        private static final String PUNCTUATION = "[\\[\\](){ }!?.,:;\"\\\\/*&^%$_+\\-–—=<>@|~]";
        private static final String ELLIPSIS = "\\.{3}";
        private static final String WORDS = "[a-zA-Zа-яА-ЯёЁ]+";
        private static final String COMPOUNDS = WORDS + "-" + WORDS;

        private static final Pattern TOKENIZE_RE = Pattern.compile(
                            String.format("(%s|%s|%s|%s|%s)", 
                                ELLIPSIS, COMPOUNDS, WORDS, PUNCTUATION, NEWLINE_PLACEHOLDER)
                        );
        
        public static List<String> tokenize(String text) {
            String paragraphed = NEWLINES_RE.matcher(text).replaceAll(NEWLINE_PLACEHOLDER);

            return TOKENIZE_RE.matcher(paragraphed)
                .results()
                .map(match -> match.group())
                .filter(token -> !token.isBlank())
                .collect(Collectors.toList());
        }
        public static String textify(List<String> tokens) {
            StringBuilder sb = new StringBuilder();
            for (String token : tokens) {
                if (token.equals(NEWLINE_PLACEHOLDER)) {
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
                        sb.setLength(sb.length() - 1);
                    }
                    sb.append(PARAGRAPH_CHARACTER);
                    continue;
                }

                if (sb.length() > 0 && isWord(token) && !lastCharIsNewline(sb)) {
                    if (sb.charAt(sb.length() - 1) != ' ') {
                        sb.append(" ");
                    }
                }

                sb.append(token);
            }
            return sb.toString().trim();
        }
        private static boolean lastCharIsNewline(StringBuilder sb) {
            return sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n';
        }
        private static boolean isWord(String token) {
            // Токен считается словом => требует перед собой пробела, 
            // если он начинается с буквы, цифры или открывающей скобки/кавычки
            return token.matches("[a-zA-Zа-яА-ЯёЁ0-9«\"\\(].*");
        }

    }

}
