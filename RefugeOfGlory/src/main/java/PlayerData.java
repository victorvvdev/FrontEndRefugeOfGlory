public class PlayerData {
    private static PlayerData instance;
    private String selectedCharacter; // "BARBARO" ou "ESPADACHIM"

    private PlayerData() {}

    public static PlayerData getInstance() {
        if (instance == null) {
            instance = new PlayerData();
        }
        return instance;
    }

    public String getSelectedCharacter() {
        return selectedCharacter;
    }

    public void setSelectedCharacter(String selectedCharacter) {
        this.selectedCharacter = selectedCharacter;
        System.out.println("PlayerData: Personagem salvo -> " + selectedCharacter);
    }

    public void clear() {
        selectedCharacter = null;
    }
}