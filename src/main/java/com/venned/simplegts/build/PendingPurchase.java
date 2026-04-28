package com.venned.simplegts.build;


public class PendingPurchase {
    public enum Type { ITEM, POKEMON }
    private final Type type;
    private final ItemGTS item;
    private final PokemonGTS pokemon;

    private PendingPurchase(Type type, ItemGTS item, PokemonGTS pokemon) {
        this.type = type;
        this.item = item;
        this.pokemon = pokemon;
    }

    public static PendingPurchase of(ItemGTS item) {
        return new PendingPurchase(Type.ITEM, item, null);
    }
    public static PendingPurchase of(PokemonGTS poke) {
        return new PendingPurchase(Type.POKEMON, null, poke);
    }

    public Type getType() { return type; }
    public ItemGTS getItem() { return item; }
    public PokemonGTS getPokemon() { return pokemon; }
}
