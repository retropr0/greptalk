import groovyx.gpars.GParsPool
import parser.WunschlisteParser
import storage.Storage
import storage.Talk
import storage.h2.H2Storage

class GrepTalk {


    static void main(String[] args) {

        Storage storage = new H2Storage("productionData", "sa", "sa", false)

        List<String> shows = ["anne-will", "hart-aber-fair", "maischberger-2016", "maybrit-illner"]

        shows.each { String show ->
            List<String> episodes = WunschlisteParser.getEpisodeUrls(show)
            GParsPool.withPool(8) {
                episodes.eachParallel { String episodeUrl ->
                    if (!storage.alreadyParsed("https://www.wunschliste.de" + episodeUrl)) {
                        def parser = new WunschlisteParser("https://www.wunschliste.de" + episodeUrl)

                        Talk talk = parser.getTalk()
                        println "Processing Talk " + talk.toString()

                        storage.addTalk(talk)
                    } else {
                        println "Skipping " + episodeUrl
                    }
                }
            }
        }

    }

}