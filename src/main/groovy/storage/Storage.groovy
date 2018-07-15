package storage

interface Storage {
    void createStructure()
    void destroyStructure()
    boolean alreadyParsed(String url)
    Talk addTalk(Talk talk)
    List<Talk> getTalks()
}