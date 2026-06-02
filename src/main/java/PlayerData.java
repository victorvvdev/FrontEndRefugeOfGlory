public class PlayerData {

    private static PlayerData instance;

    private String selectedCharacter; // "BARBARO" ou "ESPADACHIM"
    private long   characterId;       // ID retornado pela API após criação
    private String characterName;     // Nome escolhido pelo jogador

    private PlayerData() {}

    public static PlayerData getInstance() {
        if (instance == null) instance = new PlayerData();
        return instance;
    }

    public void setSelectedCharacter(String selectedCharacter) {
        this.selectedCharacter = selectedCharacter;
        System.out.println("PlayerData: Personagem salvo -> " + selectedCharacter);
    }

    public String getSelectedCharacter() { return selectedCharacter; }

    public void setCharacterId(long id)       { this.characterId = id; }
    public long  getCharacterId()             { return characterId; }

    public void setCharacterName(String name) { this.characterName = name; }
    public String getCharacterName()          { return characterName; }

    /** Classe no formato que a API espera: "BARBARIAN" ou "SWORDSMAN" */
    public String getCharacterClassForApi() {
        if ("BARBARO".equals(selectedCharacter))    return "BARBARIAN";
        if ("ESPADACHIM".equals(selectedCharacter)) return "SWORDSMAN";
        return selectedCharacter;
    }

    public void clear() {
        selectedCharacter = null;
        characterId       = 0;
        characterName     = null;
    }
}