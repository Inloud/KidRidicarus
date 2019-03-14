package kidridicarus.game.agent.SMB.NPC.turtle;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.common.info.UInfo;
import kidridicarus.game.agent.SMB.NPC.turtle.Turtle.MoveState;
import kidridicarus.game.info.SMBAnim;

public class TurtleSprite extends Sprite {
	private static final float SPRITE_WIDTH = UInfo.P2M(16);
	private static final float SPRITE_HEIGHT = UInfo.P2M(24);
	private static final float ANIM_SPEED = 0.25f;

	private Animation<TextureRegion> walkAnim;
	private TextureRegion insideShell;
	private Animation<TextureRegion> wakeUpAnim;
	private float stateTimer;

	public TurtleSprite(TextureAtlas atlas, Vector2 position) {
		walkAnim = new Animation<TextureRegion>(ANIM_SPEED,
				atlas.findRegions(SMBAnim.NPC.TURTLE_WALK), PlayMode.LOOP);
		wakeUpAnim = new Animation<TextureRegion>(ANIM_SPEED,
				atlas.findRegions(SMBAnim.NPC.TURTLE_WAKEUP), PlayMode.LOOP);
		insideShell = atlas.findRegion(SMBAnim.NPC.TURTLE_HIDE);

		stateTimer = 0;

		setRegion(walkAnim.getKeyFrame(0f));
		setBounds(getX(), getY(), SPRITE_WIDTH, SPRITE_HEIGHT);
		setPosition(position.x - getWidth()/2f, position.y - getHeight()/2f);
	}

	public void update(float delta, Vector2 position, MoveState parentState, boolean facingRight) {
		switch(parentState) {
			case NONE:
			case WALK:
				setRegion(walkAnim.getKeyFrame(stateTimer));
				break;
			case HIDE:
			case SLIDE:
				setRegion(insideShell);
				break;
			case WAKE_UP:
				setRegion(wakeUpAnim.getKeyFrame(stateTimer));
				break;
			case DEAD:
				setRegion(insideShell);
				// upside down when dead
				if(!isFlipY())
					flip(false,  true);
				break;
		}

		// Ensure the sprite is facing the correct direction
		// (the turtle sprite faces left in the spritesheet, so left/right is reversed)
		if((facingRight && !isFlipX()) || (!facingRight && isFlipX()))
			flip(true, false);

		setPosition(position.x - getWidth() / 2f, position.y - getHeight() * 0.375f);

		stateTimer += delta;
	}
}