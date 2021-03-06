package kidridicarus.agency.agentsprite;

import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.tool.FrameTime;

/*
 * Does not include flag for visible, because a null SpriteFrameInput signals non-visibility status.
 * If the sprite is non-visible then the other information about the sprite is unnecessary.
 * If a secondary visibility flag is required then implement this functionality in a separate class/subclass.
 */
public class SpriteFrameInput {
//	public class FrameSpot {
	public Vector2 position;
	public float rotation;
	public boolean flipX;	// flipX = !isFacingRight;
	public boolean flipY;	// flipY = !isFacingUp;
//	}
	public FrameTime frameTime;

	public SpriteFrameInput() {
		position = new Vector2();
		rotation = 0f;
		flipX = false;
		flipY = false;
		frameTime = new FrameTime();
	}

	public SpriteFrameInput(FrameTime frameTime, boolean flipX, boolean flipY, float rotation, Vector2 position) {
		this.position = position;
		this.rotation = rotation;
		this.flipX = flipX;
		this.flipY = flipY;
		this.frameTime = frameTime;
	}

	public SpriteFrameInput cpy() {
		SpriteFrameInput frameOut = new SpriteFrameInput();
		frameOut.position = this.position.cpy();
		frameOut.rotation = this.rotation;
		frameOut.flipX = this.flipX;
		frameOut.flipY = this.flipY;
		frameOut.frameTime = new FrameTime(this.frameTime.timeDelta, this.frameTime.timeAbs);
		return frameOut;
	}
}
