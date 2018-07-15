package parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import storage.Channel
import storage.Guest
import storage.Show
import storage.Talk

class WunschlisteParser extends AbstractWebParser {

    WunschlisteParser(String url) {
        super(url)
        gather()
    }

    @Override
    Talk getTalk() {
        Document document = Jsoup.parse(this.content)
        Show show = new Show()
        show.name = document.select("#top h1").text()

        Talk talk = new Talk()
        talk.url = this.url.toString()
        talk.show = show
        Element episodeDetail = document.select("#episodendetail").first()
        talk.episode = Integer.parseInt(episodeDetail.select(".epinfo").first().text().replaceAll(/[^0-9]/, "") ?: "0")
        talk.synopsis = episodeDetail.select(".clear.upfront.text").first().text()

        String premiere = episodeDetail.select("h6:contains(Erstausstrahlungen)").first().nextElementSibling().text()
        talk.aired = getAirDate(premiere)
        talk.channel = new Channel(name: getChannelName(premiere))
        talk.guests = getGuests()

        return talk
    }

    static Date getAirDate(String premiere) {
        String date = premiere.split(": ")[1].split(" ")[0]
        return new Date().parse("dd.MM.yyyy", date)
    }

    static String getChannelName(String premiere) {
        return premiere.split("\\(")[1].split("\\)")[0]
    }

    @Override
    List<Guest> getGuests() {
        Document document = Jsoup.parse(this.content)
        Elements cast = document.select("#cast .absatz strong")
        ArrayList<Guest> guests = []

        cast.each { Element guest ->
            guests << new Guest(name: guest.text().split("\\(")[0], detail: getGuestDetail(guest))
        }

        return guests
    }

    static String getGuestDetail(Element guest) {
        String detail = ""
        Element nextElement = guest.nextElementSibling()
        if (nextElement?.tagName() == "em") {
            detail = nextElement.text().replaceAll(/\(|\)/, "")
        } else {
            def nameAndDetail = guest.text().split("\\(")
            if (nameAndDetail.size() > 1) {
                detail = nameAndDetail[1]
            }
        }
        return detail
    }

    static List<String> getEpisodeUrls(String show) {
        Document document = Jsoup.parse("https://www.wunschliste.de/serie/$show/episoden".toURL().text)
        List<String> episodes = document.select("li[id^=opt_u_] .epl3 > a").collect {it.attr("href")}

        return episodes
    }
}
