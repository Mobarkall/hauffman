import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage : java Main <directory>");
            System.exit(1);
        }

        String directoryPath = args[0];
        File directory = new File(directoryPath);

        if (!directory.isDirectory()) {
            System.out.println("Le chemin spécifié n'est pas un répertoire.");
            System.exit(1);
        }

        // Création du répertoire "compressed" s'il n'existe pas encore
        File compressedDirectory = new File("compressed");
        if (!compressedDirectory.exists()) {
            compressedDirectory.mkdir();  // Crée le répertoire "compressed"
        }

        // Compression
        try {
            String commonHeader = extractCommonHeader(directory);
            if (commonHeader == null) {
                System.out.println("Aucun header commun trouvé.");
                System.exit(1);
            }

            // Compression du header commun
            HashMap<Character, Integer> headerOccurrences = Huffman.countCharacters(commonHeader.toCharArray());
            Huffman.Node<Character> headerTree = Huffman.createTree(headerOccurrences);
            Map<Character, String> huffmanCodeHeader = new HashMap<>();
            generateHuffmanCodes(headerTree, "", huffmanCodeHeader);

            String compressedHeaderPath = "compressed/header_compressed.huff";
            createCompressedFileFromString(commonHeader, compressedHeaderPath, huffmanCodeHeader);
            System.out.println("Header compressé créé : " + compressedHeaderPath);

            long originalSizeTotal = 0;
            long compressedSizeTotal = new File(compressedHeaderPath).length();

            // Compression des bodies des fichiers HTML
            for (File file : Objects.requireNonNull(directory.listFiles((dir, name) -> name.endsWith(".html")))) {
                String body = extractBody(file);
                HashMap<Character, Integer> bodyOccurrences = Huffman.countCharacters(body.toCharArray());
                Huffman.Node<Character> bodyTree = Huffman.createTree(bodyOccurrences);
                Map<Character, String> huffmanCodeBody = new HashMap<>();
                generateHuffmanCodes(bodyTree, "", huffmanCodeBody);

                String compressedBodyPath = "compressed/" + file.getName().replace(".html", "_body_compressed.huff");
                createCompressedFileFromString(body, compressedBodyPath, huffmanCodeBody);
                System.out.println("Body compressé pour " + file.getName() + " : " + compressedBodyPath);

                originalSizeTotal += file.length();
                compressedSizeTotal += new File(compressedBodyPath).length();
            }

            System.out.println("Taille totale des fichiers originaux : " + originalSizeTotal + " octets");
            System.out.println("Taille totale des fichiers compressés : " + compressedSizeTotal/8 + " octets");
            double compressionRatio = ((double) compressedSizeTotal/8 / originalSizeTotal) * 100;
            System.out.println("Ratio de compression : " + compressionRatio + "%");

        } catch (IOException e) {
            System.out.println("Erreur lors de la compression : " + e.getMessage());
            System.exit(1);
        }

        // Décompression
        try {
            decompressAndCombine(directoryPath);
        } catch (IOException e) {
            System.out.println("Erreur lors de la décompression : " + e.getMessage());
        }
    }

    public static void decompressAndCombine(String directoryPath) throws IOException {
        File compressedDirectory = new File("compressed");
        File decompressedDirectory = new File("decompressed");

        if (!decompressedDirectory.exists()) {
            decompressedDirectory.mkdir();  // Crée le répertoire "decompressed"
        }

        String headerCompressedFile = "compressed/header_compressed.huff";
        String decompressedHeaderPath = "decompressed/header_decompressed.html";
        decompressFile(headerCompressedFile, decompressedHeaderPath);
        String commonHeader = getFileContent(decompressedHeaderPath);

        for (File file : Objects.requireNonNull(compressedDirectory.listFiles((dir, name) -> name.endsWith("_body_compressed.huff")))) {
            String decompressedBodyPath = "decompressed/" + file.getName().replace("_body_compressed.huff", "_decompressed_body.html");
            decompressFile(file.getPath(), decompressedBodyPath);

            String finalDecompressedPath = decompressedBodyPath.replace("_decompressed_body.html", "_final_decompressed.html");
            combineHeaderBody(decompressedHeaderPath, decompressedBodyPath, finalDecompressedPath);

            System.out.println("Fichier décompressé et combiné : " + finalDecompressedPath);
        }
    }

    /**
     * Génère les codes de Huffman en parcourant l'arbre
     */
    public static void generateHuffmanCodes(Huffman.Node<Character> root, String code, Map<Character, String> huffmanCode) {
        if (root == null) {
            return;
        }

        if (root.left == null && root.right == null) {
            huffmanCode.put(root.value, code);
        }

        generateHuffmanCodes(root.left, code + "0", huffmanCode);
        generateHuffmanCodes(root.right, code + "1", huffmanCode);
    }

    public static String extractCommonHeader(File directory) throws IOException {
        String commonHeader = null;
        for (File file : Objects.requireNonNull(directory.listFiles((dir, name) -> name.endsWith(".html")))) {
            String header = extractHeader(file);
            if (commonHeader == null) {
                commonHeader = header;
            } else if (!header.equals(commonHeader)) {
                System.out.println("Les headers ne sont pas identiques dans tous les fichiers.");
                return null;
            }
        }
        return commonHeader;
    }

    public static String extractHeader(File file) throws IOException {
        StringBuilder header = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null && !line.contains("<body>")) {
                header.append(line).append("\n");
            }
        }
        return header.toString();
    }

    public static String extractBody(File file) throws IOException {
        StringBuilder body = new StringBuilder();
        boolean inBody = false;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("<body>")) {
                    inBody = true;
                }
                if (inBody) {
                    body.append(line).append("\n");
                }
            }
        }
        return body.toString();
    }

    public static void createCompressedFileFromString(String content, String compressedFilePath, Map<Character, String> huffmanCode) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(compressedFilePath));

        writer.write(huffmanCode.size() + "\n");
        for (Map.Entry<Character, String> entry : huffmanCode.entrySet()) {
            writer.write(escapeForFile(entry.getKey()) + "->" + entry.getValue() + "\n");
        }

        StringBuilder encodedString = new StringBuilder();
        for (char c : content.toCharArray()) {
            encodedString.append(huffmanCode.get(c));
        }
        writer.write(encodedString.toString());

        writer.close();
    }

    private static String escapeForFile(char character) {
        switch (character) {
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            case '€': return "\\u20AC";
            default: return String.valueOf(character);
        }
    }

    private static char unescapeFromFile(String escaped) {
        switch (escaped) {
            case "\\n": return '\n';
            case "\\r": return '\r';
            case "\\t": return '\t';
            case "\\u20AC": return '€';
            default: return escaped.charAt(0);
        }
    }

    public static void decompressFile(String compressedFilePath, String decompressedFilePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(compressedFilePath));
        String line = reader.readLine();
        int dictionarySize = Integer.parseInt(line.trim());
        Map<String, Character> reverseHuffmanCode = new HashMap<>();

        for (int i = 0; i < dictionarySize; i++) {
            line = reader.readLine();
            String[] entry = line.split("->", 2);
            String code = entry[1];
            char character = unescapeFromFile(entry[0]);
            reverseHuffmanCode.put(code, character);
        }

        StringBuilder compressedData = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            compressedData.append(line);
        }
        reader.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(decompressedFilePath));
        StringBuilder currentCode = new StringBuilder();
        for (char bit : compressedData.toString().toCharArray()) {
            currentCode.append(bit);
            if (reverseHuffmanCode.containsKey(currentCode.toString())) {
                writer.write(reverseHuffmanCode.get(currentCode.toString()));
                currentCode.setLength(0);
            }
        }
        writer.close();
    }

    public static void combineHeaderBody(String headerFile, String bodyFile, String outputFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.write(getFileContent(headerFile));
        writer.write(getFileContent(bodyFile));
        writer.close();
    }

    public static String getFileContent(String filepath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }
}
