package io.github.sts8.vanitygen.bruteforce;

import java.util.Arrays;

import static io.github.sts8.vanitygen.model.SyncthingIdentity.SYNCTHING_ID_ALPHABET;

/**
 * A high-performance Trie optimized for Syncthing vanity generation.
 * <p>
 * This version supports the Syncthing Base32 alphabet plus the hyphen '-' character.
 * It uses an ASCII lookup table to eliminate string searching during the brute-force loop.
 */
public class PrefixTrie {

    /**
     * 32 Base32 characters + 1 hyphen = 33 slots.
     */
    private static final int ALPHABET_SIZE = SYNCTHING_ID_ALPHABET.length() + 1;

    /**
     * Fast lookup for ASCII character indices.
     */
    private static final int[] INDEX_MAP = new int[128];

    static {
        Arrays.fill(INDEX_MAP, -1);
        for (int i = 0; i < SYNCTHING_ID_ALPHABET.length(); i++) {
            INDEX_MAP[SYNCTHING_ID_ALPHABET.charAt(i)] = i;
        }
        // Hyphen is at the final index (32)
        INDEX_MAP['-'] = SYNCTHING_ID_ALPHABET.length();
    }

    private final Node root = new Node();

    /**
     * Inserts a prefix into the Trie.
     * Hyphens are treated as literal characters.
     *
     * @param prefix the vanity prefix to register.
     */
    public void insert(String prefix) {
        Node node = root;
        String upper = prefix.toUpperCase();
        for (int i = 0; i < upper.length(); i++) {
            int idx = getIndex(upper.charAt(i));
            if (idx == -1) continue;

            if (node.children[idx] == null) {
                node.children[idx] = new Node();
            }
            node = node.children[idx];
        }
        node.terminal = true;
    }

    /**
     * Checks if the provided value starts with any registered prefix.
     *
     * @param value the formatted Syncthing ID (including hyphens).
     * @return true if a match is found.
     */
    public boolean matches(String value) {
        Node node = root;
        for (int i = 0; i < value.length(); i++) {
            int idx = getIndex(value.charAt(i));

            if (idx == -1 || node.children[idx] == null) {
                return false;
            }

            node = node.children[idx];

            if (node.terminal) {
                return true;
            }
        }
        return false;
    }

    private int getIndex(char c) {
        return (c < 128) ? INDEX_MAP[c] : -1;
    }

    private static class Node {
        private final Node[] children = new Node[ALPHABET_SIZE];
        private boolean terminal = false;
    }
}
