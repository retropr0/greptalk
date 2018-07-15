package storage.h2

import groovy.sql.Sql
import storage.*

class H2Storage implements Storage {

    Sql sql

    H2Storage(String fileName, String user, String password, boolean drop) {
        Class.forName("org.h2.Driver")
        sql = Sql.newInstance("jdbc:h2:file:./databases/$fileName", user, password, "org.h2.Driver")
        if (drop) {
            this.destroyStructure()
        }
        this.createStructure()
    }

    @Override
    void createStructure() {
        this.sql.execute("create table IF NOT EXISTS talks (id INT PRIMARY KEY AUTO_INCREMENT, show_id INT, episode INT, synopsis TEXT, aired DATETIME, channel_id INT, url VARCHAR(255))")
        this.sql.execute("create table IF NOT EXISTS shows (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100))")
        this.sql.execute("create table IF NOT EXISTS channels (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100))")
        this.sql.execute("create table IF NOT EXISTS guests (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), detail VARCHAR(255))")
        this.sql.execute("create table IF NOT EXISTS talk_guests (talk_id INT, guest_id INT)")
    }

    @Override
    void destroyStructure() {
        this.sql.execute("DROP TABLE if exists talks")
        this.sql.execute("DROP TABLE if exists shows")
        this.sql.execute("DROP TABLE if exists channels")
        this.sql.execute("DROP TABLE if exists guests")
        this.sql.execute("DROP TABLE if exists talk_guests")
    }

    @Override
    Talk addTalk(Talk talk) {

        talk.channel.id = findOrCreateChannel(talk.channel)
        talk.show.id = findOrCreateShow(talk.show)

        def talkRow = this.sql.firstRow("select * from TALKS where SHOW_ID = :show_id and CHANNEL_ID = :channel_id and EPISODE = :episode and AIRED = :aired", [show_id: talk.show.id, channel_id: talk.channel.id, episode: talk.episode, aired: talk.aired])

        if (!talkRow) {
            println "Creating Talk " + talk.toString()
            talk.id = this.sql.executeInsert("insert into TALKS (SHOW_ID, EPISODE, SYNOPSIS, AIRED, channel_id, url) values (:show_id, :episode, :synopsis, :aired, :channel_id, :url)", [show_id: talk.show.id, episode: talk.episode, synopsis: talk.synopsis, aired: talk.aired, channel_id: talk.channel.id, url: talk.url]).first().first() as int
            addTalkGuests(talk, talk.guests)
        } else {
            talk.id = talkRow.id
        }

        return talk
    }

    @Override
    boolean alreadyParsed(String url) {
        return this.sql.rows("select * from TALKS where url = :url", [url: url]).size() == 1
    }

    int findOrCreateShow(Show show) {
        def row = this.sql.firstRow("select * from SHOWS where NAME like :name", [name: show.name])

        if (!row) {
            return this.sql.executeInsert("insert into SHOWS set NAME = :name", [name: show.name]).first().first() as int
        }

        return row.id
    }

    int findOrCreateChannel(Channel channel) {

        def row = this.sql.firstRow("select * from CHANNELS where name like :name", [name: channel.name ?: "Unknown"])

        if (!row) {
            return this.sql.executeInsert("insert into CHANNELS set name = :name", [name: channel.name]).first().first() as int
        }

        return row.id
    }

    int findOrCreateGuest(Guest guest) {
        def row = this.sql.firstRow("select * from GUESTS where name like :name and detail = :detail", [name: guest.name, detail: guest.detail])

        if (!row) {
            return this.sql.executeInsert("insert into GUESTS (NAME, DETAIL)values (:name, :detail)", [name: guest.name, detail: guest.detail]).first().first() as int

        }

        return row.id
    }

    void addTalkGuests(Talk talk, List<Guest> guests) {
        guests.each { Guest guest ->
            guest.id = findOrCreateGuest(guest)
            this.sql.execute("insert into TALK_GUESTS values (:talk, :guest)", [talk: talk.id, guest: guest.id])
        }
    }

    @Override
    List<Talk> getTalks() {
        ArrayList<Talk> talks = []
        def rows = this.sql.rows("select * from TALKS")
        rows.each { talkrow ->
            Show show = getShowById(talkrow.show_id)
            Channel channel = getChannelById(talkrow.channel_id)
            List<Guest> guests = getGuestsByTalk(talkrow.id)
            talks << new Talk(id: talkrow.id,
                                show: show,
                                episode: talkrow.episode,
                                synopsis: talkrow.synopsis,
                                guests: guests,
                                aired: talkrow.aired,
                                channel: channel)
        }
        return talks
    }

    Show getShowById(Integer id) {
        def row = this.sql.firstRow("select * from SHOWS where ID = :id", [id: id])
        return new Show(id: row.id, name: row.name)
    }

    Channel getChannelById(Integer id) {
        def row = this.sql.firstRow("select * from CHANNELS where ID = :id", [id: id])
        return new Channel(id: row.id, name: row.name)
    }

    List<Guest> getGuestsByTalk(Integer id) {
        ArrayList<Guest> guests = []
        def guestIdList = this.sql.rows("select * from TALK_GUESTS where talk_id = :id", [id: id]).collect {it.guest_id}
        if (guestIdList) {
            def rows = this.sql.rows("select * from GUESTS g where g.ID in (${guestIdList.join(",")})".toString())
            rows.each { row ->
                guests.add(new Guest(id: row.id, name: row.name, detail: row.detail))
            }
        }
        return guests
    }
}