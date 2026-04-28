package com.venned.simplegts.build;

import java.util.UUID;

public class PokemonGTS {

    UUID owner;
    String specie;
    int level;
    boolean shiny;
    String ivs;
    String evs;
    String ability;
    String benchedMoves;

    int price;
    long putInMerch;

    public PokemonGTS(UUID owner,  String specie, int level, boolean shiny, String ivs, String evs, String ability, String benchedMoves, Long putInMerch, int price) {
        this.owner = owner;
        this.specie = specie;
        this.level = level;
        this.shiny = shiny;
        this.ivs = ivs;
        this.evs = evs;
        this.ability = ability;
        this.benchedMoves = benchedMoves;
        this.putInMerch = putInMerch;
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

    public long getPutInMerch() {
        return putInMerch;
    }

    public UUID getOwner() {
        return owner;
    }
    public String getSpecie() {
        return specie;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void setSpecie(String specie) {
        this.specie = specie;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isShiny() {
        return shiny;
    }

    public void setShiny(boolean shiny) {
        this.shiny = shiny;
    }

    public String getIvs() {
        return ivs;
    }

    public void setIvs(String ivs) {
        this.ivs = ivs;
    }

    public String getEvs() {
        return evs;
    }

    public void setEvs(String evs) {
        this.evs = evs;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }

    public String getBenchedMoves() {
        return benchedMoves;
    }

    public void setBenchedMoves(String benchedMoves) {
        this.benchedMoves = benchedMoves;
    }
}
