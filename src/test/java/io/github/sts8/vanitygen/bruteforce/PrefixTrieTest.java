package io.github.sts8.vanitygen.bruteforce;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefixTrieTest {

    private static final String SYNCTHING_ID_1 = "7777777-7777777-7777777-7777777-7777777-7777777-7777777-7777777";
    private static final String SYNCTHING_ID_2 = "AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA";

    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
        trie = new PrefixTrie();
    }

    @Test
    void matchesStandardSyncthingBlock() {
        trie.insert("777");
        assertTrue(trie.matches(SYNCTHING_ID_1));
        assertFalse(trie.matches(SYNCTHING_ID_2));
    }

    @Test
    void matchesAcrossMultipleBlocks() {
        trie.insert("7777777-7");
        assertTrue(trie.matches(SYNCTHING_ID_1));
    }

}