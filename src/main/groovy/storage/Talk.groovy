package storage

class Talk {
    Integer id
    Show show
    Integer episode
    List<Guest> guests
    String synopsis
    Date aired
    Channel channel
    String url

    @Override
    String toString() {
        return this.show.name + " " + this.episode + " (" + this.aired?.format("dd.MM.yyyy HH:mm") + ")"
    }
}

