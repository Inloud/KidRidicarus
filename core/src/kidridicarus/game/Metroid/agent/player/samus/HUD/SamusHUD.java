package kidridicarus.game.Metroid.agent.player.samus.HUD;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import kidridicarus.agency.Agent;
import kidridicarus.common.agent.playeragent.playerHUD.PlayerHUD;
import kidridicarus.common.agent.playeragent.playerHUD.TexRegionActor;
import kidridicarus.game.Metroid.MetroidGfx;
import kidridicarus.game.Metroid.MetroidKV;
import kidridicarus.game.SMB1.SMB1_Gfx;

public class SamusHUD extends PlayerHUD {
	private Agent playerAgent;
	private TextureAtlas atlas;
	private Label energyAmountLabel;

	public SamusHUD(Agent playerAgent, TextureAtlas atlas) {
		this.playerAgent = playerAgent;
		this.atlas = atlas;
	}

	@Override
	public void setupStage(Stage stage) {
		Table table = new Table();
		table.top();
		table.setFillParent(true);

		LabelStyle labelstyle = new Label.LabelStyle(new BitmapFont(Gdx.files.internal(SMB1_Gfx.SMB1_FONT), false),
				Color.WHITE);
		energyAmountLabel = new Label(String.format("%02d", 0), labelstyle);

		table.add(new TexRegionActor(atlas.findRegion(MetroidGfx.Player.HUD.ENERGY_TEXT))).
				align(Align.left).padLeft(24).padTop(16);
		
		table.add(energyAmountLabel).align(Align.left).expandX().padTop(16);

		stage.addActor(table);
	}

	@Override
	protected void preDrawStage() {
		energyAmountLabel.setText(String.format("%02d",
				playerAgent.getProperty(MetroidKV.KEY_ENERGY_SUPPLY, 0, Integer.class)));
	}
}
