import de.gurkenlabs.litiengine.Game;

public class Main {
    public static void main(String[] args) {

        Game.info().setName("Refuge Of Glory");
        Game.info().setVersion("v0.1");
        Game.init(args);

        // Telas de login / cadastro
        Game.screens().add(new LoginScreen());
        Game.screens().add(new RegisterScreen());

        // Telas de introdução narrativa
        Game.screens().add(new IntroNarrativeScreen());
        Game.screens().add(new CharacterSelectScreen());
        Game.screens().add(new CharacterStoryScreen());

        // Primeira batalha e transições
        Game.screens().add(new PreBattleNarrativeScreen());
        Game.screens().add(new FirstBattleScreen());
        Game.screens().add(new PostFirstBattleScreen());

        // Cabana da herbalista, batalha do Golem e pós‑batalha
        Game.screens().add(new HerbalistNarrativeScreen());
        Game.screens().add(new GolemBattleScreen());
        Game.screens().add(new PostGolemBattleScreen());

        // Batalha da Pantera Deslocadora e pós‑batalha
        Game.screens().add(new PantherBattleScreen());
        Game.screens().add(new PostPantherBattleScreen());

        // Encontro com o Ancião e batalha final
        Game.screens().add(new ElderNarrativeScreen());
        Game.screens().add(new FinalBattleScreen());

        // ── NOVA TELA DE PÓS‑BATALHA FINAL ─────────────────────────
        Game.screens().add(new PostFinalBattleScreen());

        // Tela final de encerramento
        Game.screens().add(new FinalNarrativeScreen());

        // Inicia na tela de login
        Game.screens().display("LOGIN");
        Game.start();
    }
}