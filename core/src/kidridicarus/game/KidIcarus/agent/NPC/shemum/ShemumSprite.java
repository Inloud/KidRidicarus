package kidridicarus.game.KidIcarus.agent.NPC.shemum;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.agentsprite.AgentSprite;
import kidridicarus.agency.agentsprite.SpriteFrameInput;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.SprFrameTool;
import kidridicarus.game.KidIcarus.KidIcarusGfx;

class ShemumSprite extends AgentSprite {
	private static final float SPRITE_WIDTH = UInfo.P2M(16);
	private static final float SPRITE_HEIGHT = UInfo.P2M(16);
	private static final float ANIM_SPEED = 2/15f;

	private Animation<TextureRegion> anim;
	private float animTimer;

	ShemumSprite(TextureAtlas atlas, Vector2 position) {
		anim = new Animation<TextureRegion>(ANIM_SPEED, atlas.findRegions(KidIcarusGfx.NPC.SHEMUM), PlayMode.LOOP);
		animTimer = 0;
		setRegion(anim.getKeyFrame(0f));
		setBounds(getX(), getY(), SPRITE_WIDTH, SPRITE_HEIGHT);
		postFrameInput(SprFrameTool.place(position));
	}

	@Override
	public void processFrame(SpriteFrameInput frameInput) {
		if(!preFrameInput(frameInput))
			return;
		animTimer += frameInput.frameTime.timeDelta;
		setRegion(anim.getKeyFrame(animTimer));
		postFrameInput(frameInput);
	}
}
