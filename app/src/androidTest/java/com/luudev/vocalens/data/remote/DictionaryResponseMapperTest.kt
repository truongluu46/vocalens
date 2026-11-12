package com.luudev.vocalens.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luudev.vocalens.data.mapper.toEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryResponseMapperTest {
    @Test fun selectsVietnameseAndBritishIpaAndPreservesExample() {
        val json = """{
          "word":"hello","entries":[{"language":{"code":"en"},"partOfSpeech":"interjection",
          "pronunciations":[{"type":"ipa","text":"US","tags":["General American"]},
            {"type":"ipa","text":"UK","tags":["Received Pronunciation"]}],
          "senses":[{"definition":"A greeting","examples":["Hello, everyone."],
            "translations":[{"language":{"code":"fr"},"word":"bonjour"},
              {"language":{"code":"vi"},"word":"xin chào"}],
            "subsenses":[{"definition":"On the telephone","translations":[
              {"language":{"code":"vi"},"word":"a-lô"}]}]}]}],
          "source":{"url":"https://en.wiktionary.org/wiki/hello",
            "license":{"name":"CC BY-SA 4.0","url":"https://creativecommons.org/licenses/by-sa/4.0/"}}
        }"""
        val results = DictionaryResponseMapper.map(json)
        assertEquals(2, results.size)
        val entity = results.first().word.toEntity(123L)
        assertEquals("xin chào", entity.meaningVi)
        assertEquals("UK", entity.phonetic)
        assertEquals("interjection", entity.partOfSpeech)
        assertEquals("Hello, everyone.", entity.exampleSentence)
        assertEquals("xin chào", results.last().word.meaning)
        assertEquals("US", results.last().word.phonetic)
        assertNotEquals(results.first().word.id, results.last().word.id)
        assertEquals(results.first().word.id, DictionaryResponseMapper.map(json).first().word.id)
        assertEquals("CC BY-SA 4.0", results.first().licenseName)
    }

    @Test fun missingTranslationsDoesNotTreatEnglishDefinitionAsVietnamese() {
        val results = DictionaryResponseMapper.map("""{
          "word":"test","entries":[{"language":{"code":"en"},
          "senses":[{"definition":"An examination"}]}]
        }""")
        assertEquals("", results.single().word.meaning)
        assertEquals("An examination", results.single().definition)
    }
}
