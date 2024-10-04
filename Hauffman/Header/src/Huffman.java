import java.io.*;
import java.util.*;

public class Huffman {

    static class Node<T> {
        public T value;
        public int occurences;
        public Node<T> left;
        public Node<T> right;

        public Node(T value, int occurences, Node<T> left, Node<T> right) {
            this.value = value;
            this.occurences = occurences;
            this.left = left;
            this.right = right;
        }
    }


    public static char[] getCharacters(String filepath) throws IOException {
        File file = new File(filepath);
        char[] content = new char[(int) file.length()];
        try (FileReader fr = new FileReader(file)) {
            fr.read(content);
        }
        return content;
    }


    public static HashMap<Character, Integer> countCharacters(char[] chars) {
        HashMap<Character, Integer> res = new HashMap<>();
        for (char c : chars) {
            res.put(c, res.getOrDefault(c, 0) + 1);
        }
        return res;
    }




    public static Node<Character> createTree(HashMap<Character, Integer> occurences) {
        PriorityQueue<Node<Character>> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.occurences));

        for (Map.Entry<Character, Integer> entry : occurences.entrySet()) {
            pq.add(new Node<>(entry.getKey(), entry.getValue(), null, null));
        }

        while (pq.size() > 1) {
            Node<Character> left = pq.poll();
            Node<Character> right = pq.poll();
            Node<Character> parent = new Node<>(null, left.occurences + right.occurences, left, right);
            pq.add(parent);
        }

        return pq.poll();
    }


}




