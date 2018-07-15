package parser

import storage.Guest
import storage.Talk

abstract class AbstractWebParser {

    URL url
    String content

    AbstractWebParser(String url) {
        this.url = new URL(url)
    }

    void gather() {
        content = url.text
    }

    abstract Talk getTalk()
    abstract List<Guest> getGuests()

}
