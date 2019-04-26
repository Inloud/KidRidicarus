package kidridicarus.game.KidIcarus.agent.NPC.monoeye;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agentcontact.CFBitSeq;
import kidridicarus.common.agent.fullactor.FullActorBody;
import kidridicarus.common.agentbrain.ContactDmgBrainContactFrameInput;
import kidridicarus.common.agentbrain.RoomingBrainFrameInput;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.B2DFactory;

public class MonoeyeBody extends FullActorBody {
	private static final float BODY_WIDTH = UInfo.P2M(12f);
	private static final float BODY_HEIGHT = UInfo.P2M(12f);
	private static final float PLAYER_SENSOR_WIDTH = UInfo.P2M(128);
	private static final float PLAYER_SENSOR_HEIGHT = UInfo.P2M(176);
	private static final float GRAVITY_SCALE = 0f;

	private static final CFBitSeq AS_CFCAT = new CFBitSeq(CommonCF.Alias.AGENT_BIT);
	// Contact spawn trigger to detect screen scroll (TODO create a ScreenAgent that represents the player screen
	// and allow this body to contact ScreenAgent?). 
	private static final CFBitSeq AS_CFMASK = new CFBitSeq(CommonCF.Alias.AGENT_BIT, CommonCF.Alias.DESPAWN_BIT,
			CommonCF.Alias.KEEP_ALIVE_BIT, CommonCF.Alias.SPAWNTRIGGER_BIT);

	private MonoeyeSpine spine;

	public MonoeyeBody(Monoeye parent, World world, Vector2 position, Vector2 velocity) {
		super(parent, world);
		defineBody(new Rectangle(position.x-BODY_WIDTH/2f, position.y-BODY_HEIGHT/2f, BODY_WIDTH, BODY_HEIGHT),
				velocity);
	}

	@Override
	protected void defineBody(Rectangle bounds, Vector2 velocity) {
		// dispose the old body if it exists
		if(b2body != null)
			world.destroyBody(b2body);

		setBoundsSize(bounds.width, bounds.height);
		b2body = B2DFactory.makeDynamicBody(world, bounds.getCenter(new Vector2()), velocity);
		b2body.setGravityScale(GRAVITY_SCALE);
		spine = new MonoeyeSpine(this, UInfo.M2Tx(bounds.x+bounds.width/2f));
		// agent sensor fixture
		B2DFactory.makeSensorBoxFixture(b2body, AS_CFCAT, AS_CFMASK, spine.createAgentSensor(),
				bounds.width, bounds.height);
		// create player sensor fixture that "hangs" down from the top of body and detects players to target
		B2DFactory.makeSensorBoxFixture(b2body, CommonCF.AGENT_SENSOR_CFCAT, CommonCF.AGENT_SENSOR_CFMASK,
				spine.createPlayerSensor(), PLAYER_SENSOR_WIDTH, PLAYER_SENSOR_HEIGHT,
				new Vector2(0f, bounds.height/2f - PLAYER_SENSOR_HEIGHT/2f));
	}

	@Override
	public ContactDmgBrainContactFrameInput processContactFrame() {
		return new ContactDmgBrainContactFrameInput(spine.getContactDmgTakeAgents());
	}

	@Override
	public RoomingBrainFrameInput processFrame(float delta) {
		return new RoomingBrainFrameInput(delta, spine.getCurrentRoom(), spine.isTouchingKeepAlive(),
				spine.isContactDespawn());
	}

	public MonoeyeSpine getSpine() {
		return spine;
	}
}