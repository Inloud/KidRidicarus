package kidridicarus.game.SMB1.agent.player.mariofireball;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agentbody.AgentBody;
import kidridicarus.agency.agentbody.CFBitSeq;
import kidridicarus.common.agentbrain.ContactDmgBrainContactFrameInput;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.B2DFactory;

class MarioFireballBody extends AgentBody {
	private static final float BODY_WIDTH = UInfo.P2M(5f);
	private static final float BODY_HEIGHT = UInfo.P2M(5f);
	private static final float AGENT_SENSOR_WIDTH = UInfo.P2M(8f);
	private static final float AGENT_SENSOR_HEIGHT = UInfo.P2M(8f);
	private static final float GRAVITY_SCALE = 2f;	// heavy

	private static final CFBitSeq MAIN_CFCAT = CommonCF.SOLID_BODY_CFCAT;
	private static final CFBitSeq MAIN_CFMASK = CommonCF.SOLID_BODY_CFMASK;
	private static final CFBitSeq AS_CFCAT = new CFBitSeq(CommonCF.Alias.AGENT_BIT);
	private static final CFBitSeq AS_CFMASK = new CFBitSeq(CommonCF.Alias.AGENT_BIT,
			CommonCF.Alias.DESPAWN_BIT, CommonCF.Alias.SOLID_BOUND_BIT, CommonCF.Alias.KEEP_ALIVE_BIT,
			CommonCF.Alias.ROOM_BIT);

	private MarioFireballSpine spine;
	private Vector2 prevVelocity;

	MarioFireballBody(MarioFireball parent, World world, Vector2 position, Vector2 velocity) {
		super(parent, world);
		defineBody(new Rectangle(position.x-BODY_WIDTH/2f, position.y-BODY_HEIGHT/2f, BODY_WIDTH, BODY_HEIGHT),
				velocity);
	}

	@Override
	protected void defineBody(Rectangle bounds, Vector2 velocity) {
		// dispose the old body if it exists
		if(b2body != null)
			world.destroyBody(b2body);

		setBoundsSize(BODY_WIDTH, BODY_HEIGHT);
		b2body = B2DFactory.makeDynamicBody(world, bounds.getCenter(new Vector2()), velocity);
		b2body.setGravityScale(GRAVITY_SCALE);
		b2body.setBullet(true);
		spine = new MarioFireballSpine(this);
		prevVelocity = velocity.cpy();
		// main fixture
		FixtureDef fdef = new FixtureDef();
		fdef.friction = 0f;		// slippery
		B2DFactory.makeBoxFixture(b2body, fdef, MAIN_CFCAT, MAIN_CFMASK, spine.createSolidContactSensor(),
				getBounds().width, getBounds().height);
		// agent sensor
		B2DFactory.makeSensorBoxFixture(b2body, AS_CFCAT, AS_CFMASK, spine.createAgentSensor(),
				AGENT_SENSOR_WIDTH, AGENT_SENSOR_HEIGHT);
	}

	void postUpdate() {
		prevVelocity.set(b2body.getLinearVelocity());
	}

	void setGravityScale(float scale) {
		b2body.setGravityScale(scale);
	}

	MarioFireballSpine getSpine() {
		return spine;
	}

	Vector2 getPrevVelocity() {
		return prevVelocity;
	}

	ContactDmgBrainContactFrameInput processContactFrame() {
		return new ContactDmgBrainContactFrameInput(spine.getCurrentRoom(), spine.isContactKeepAlive(),
				spine.isContactDespawn(), spine.getContactDmgTakeAgents());
	}
}
