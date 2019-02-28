package kidridicarus.game.agent.body.SMB.item;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agent.body.MobileAgentBody;
import kidridicarus.agency.agent.body.optional.BumpableBody;
import kidridicarus.agency.agent.body.sensor.SolidBoundSensor;
import kidridicarus.agency.contact.CFBitSeq;
import kidridicarus.agency.info.UInfo;
import kidridicarus.agency.tool.B2DFactory;
import kidridicarus.game.agent.SMB.item.PowerStar;
import kidridicarus.game.info.GameInfo;

public class PowerStarBody extends MobileAgentBody implements BumpableBody {
	private static final float BODY_WIDTH = UInfo.P2M(14f);
	private static final float BODY_HEIGHT = UInfo.P2M(12f);

	private PowerStar parent;
	private SolidBoundSensor hmSensor;

	public PowerStarBody(PowerStar parent, World world, Vector2 position) {
		this.parent = parent;
		defineBody(world, position);
	}

	private void defineBody(World world, Vector2 position) {
		setBodySize(BODY_WIDTH, BODY_HEIGHT);

		BodyDef bdef;
		bdef = new BodyDef();
		bdef.position.set(position);
		bdef.type = BodyDef.BodyType.DynamicBody;
		bdef.gravityScale = 0.5f;	// floaty
		FixtureDef fdef = new FixtureDef();
		fdef.restitution = 1f;	// bouncy
		// items contact mario but can pass through goombas, turtles, etc.
		CFBitSeq catBits = new CFBitSeq(GameInfo.CFBits.ITEM_BIT);
		CFBitSeq maskBits = new CFBitSeq(GameInfo.CFBits.SOLID_BOUND_BIT, GameInfo.CFBits.AGENT_BIT);
		hmSensor = new SolidBoundSensor(parent);
		b2body = B2DFactory.makeSpecialBoxBody(world, bdef, fdef, hmSensor, catBits, maskBits,
				BODY_WIDTH, BODY_HEIGHT);
	}

	@Override
	public void onBump(Agent bumpingAgent) {
		parent.onBump(bumpingAgent);
	}

	public boolean isMoveBlocked(boolean movingRight) {
		return hmSensor.isHMoveBlocked(getBounds(), movingRight);
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}
