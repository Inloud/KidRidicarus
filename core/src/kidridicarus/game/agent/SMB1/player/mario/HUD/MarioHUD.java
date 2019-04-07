package kidridicarus.game.agent.SMB1.player.mario.HUD;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import kidridicarus.agency.Agency;
import kidridicarus.common.playerHUD.AnimationActor;
import kidridicarus.game.agent.SMB1.player.mario.Mario;
import kidridicarus.game.info.SMB1_Gfx;
import kidridicarus.game.info.SMB1_KV;

public class MarioHUD implements Disposable {
	private static final float COIN_ANIM_SPEED = 0.133f;
	private Mario mario;
	private Stage stage;

	private Label scoreVarLabel;
	private Label coinVarLabel;
	private Label worldVarLabel;
	private Label timeVarLabel;
	private AnimationActor animatingCoin;
	private Agency agency;

	public MarioHUD(Agency agency, Mario agent, TextureAtlas atlas, Stage stage) {
		this.agency = agency;
		this.mario = agent;
		this.stage = stage;

		Table table = new Table();
		table.top();
		table.setFillParent(true);

		LabelStyle labelstyle = new Label.LabelStyle(new BitmapFont(Gdx.files.internal(SMB1_Gfx.SMB1_FONT), false),
				Color.WHITE);
		Label marioLabel = new Label("MARIO", labelstyle);
		Label worldLabel = new Label("WORLD", labelstyle);
		Label timeLabel = new Label("TIME", labelstyle);
		scoreVarLabel = new Label(String.format("%06d", 0), labelstyle);
		coinVarLabel = new Label(String.format("�%02d", 0), labelstyle);
		worldVarLabel = new Label("1-1", labelstyle);
		timeVarLabel = new Label(String.format("%03d", 0), labelstyle);

		table.add(marioLabel).align(Align.left).colspan(3).expandX().padLeft(24).padTop(16);
		table.add(worldLabel).align(Align.left).expandX().padTop(16);
		table.add(timeLabel).align(Align.left).expandX().padTop(16);
		table.row();
		table.add(scoreVarLabel).align(Align.left).expandX().padLeft(24);
//		table.add(new HudCoin(atlas)).align(Align.right);

		animatingCoin = new AnimationActor(new Animation<TextureRegion>(COIN_ANIM_SPEED, atlas.findRegions(SMB1_Gfx.General.HUD_COIN),
				PlayMode.LOOP));
		table.add(animatingCoin).align(Align.right);

		table.add(coinVarLabel).align(Align.left).expandX();
		table.add(worldVarLabel).align(Align.left).expandX();
		table.add(timeVarLabel).align(Align.left).expandX();

		stage.addActor(table);
	}

	public void draw() {
		update();
		stage.getBatch().setProjectionMatrix(stage.getCamera().combined);
		stage.draw();
	}

	private void update() {
		scoreVarLabel.setText(String.format("%06d",
				mario.getProperty(SMB1_KV.KEY_POINTAMOUNT, 0, Integer.class)));
//		timeVarLabel.setText(String.format("%03d", (int) mario.getLevelTimeRemaining()));
		coinVarLabel.setText(String.format("�%02d",
				mario.getProperty(SMB1_KV.KEY_COINAMOUNT, 0, Integer.class)));

		animatingCoin.setStateTimer(agency.getGlobalTimer());

		stage.act();
	}


	@Override
	public void dispose() {
		stage.dispose();
	}
}