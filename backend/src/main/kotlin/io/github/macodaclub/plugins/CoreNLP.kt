package io.github.macodaclub.plugins

/*
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import io.github.macodaclub.utils.contextReceiver
import java.util.*

fun configureCoreNLP(): String.() -> String {
    val props = Properties().apply {
        setProperty("annotators", "tokenize,pos,lemma")
    }
    val pipeline = StanfordCoreNLP(props)

    return {
        pipeline.processToCoreDocument(contextReceiver()).tokens().joinToString { it.lemma() }
    }
}*/
