package kidridicarus.game.agent.KidIcarus.player.pitarrow;

import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agentcontact.CFBitSeq;
import kidridicarus.common.agentbody.MobileAgentBody;
import kidridicarus.common.agentsensor.AgentContactHoldSensor;
import kidridicarus.common.agentsensor.SolidContactSensor;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.B2DFactory;
import kidridicarus.common.tool.Direction4;

public class PitArrowBody extends MobileAgentBody {
	private static final float BODY_WIDTH = UInfo.P2M(3);
	private static final float BODY_HEIGHT = UInfo.P2M(3);

	private static final float GRAVITY_SCALE = 0f;

	private static final CFBitSeq MAIN_CFCAT = CommonCF.SOLID_BODY_CFCAT;
	private static final CFBitSeq MAIN_CFMASK = CommonCF.SOLID_BODY_CFMASK;

	private static final CFBitSeq AS_CFCAT = new CFBitSeq(CommonCF.Alias.AGENT_BIT);
	private static final CFBitSeq AS_CFMASK = new CFBitSeq(CommonCF.Alias.AGENT_BIT, CommonCF.Alias.DESPAWN_BIT);

	private SolidContactSensor boundSensor;
	private AgentContactHoldSensor acSensor;
	private Direction4 arrowDir;

	public PitArrowBody(PitArrow parent, World world, Vector2 position, Vector2 velocity, Direction4 arrowDir) {
		super(parent, world);
		this.arrowDir = arrowDir;
		defineBody(position, velocity);
	}

	@Override
	protected void defineBody(Vector2 position, Vector2 velocity) {
		// dispose the old body if it exists	
		if(b2body != null)	
			world.destroyBody(b2body);

		if(arrowDir.isHorizontal()) {
			setBodySize(BODY_WIDTH, BODY_HEIGHT);
		}
		else
			setBodySize(BODY_HEIGHT, BODY_WIDTH);

		b2body = B2DFactory.makeDynamicBody(world, position, velocity);
		b2body.setGravityScale(GRAVITY_SCALE);
		b2body.setBullet(true);
		// create main fixture
		boundSensor = new SolidContactSensor(this);
		B2DFactory.makeBoxFixture(b2body, boundSensor, MAIN_CFCAT, MAIN_CFMASK, getBodySize().x, getBodySize().y);
		// create agent contact sensor fixture
		acSensor = new AgentContactHoldSensor(this);
		B2DFactory.makeSensorBoxFixture(b2body, acSensor, AS_CFCAT, AS_CFMASK, getBodySize().x, getBodySize().y);
	}

	public boolean isHitBound() {
		return boundSensor.isContacting();
	}

	public <T> List<T> getContactAgentsByClass(Class<T> cls) {
		return acSensor.getContactsByClass(cls);
	}
}