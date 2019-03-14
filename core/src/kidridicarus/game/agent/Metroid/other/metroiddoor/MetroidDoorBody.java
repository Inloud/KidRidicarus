package kidridicarus.game.agent.Metroid.other.metroiddoor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agentbody.AgentBody;
import kidridicarus.agency.agentcontact.AgentBodyFilter;
import kidridicarus.agency.agentcontact.CFBitSeq;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.B2DFactory;

public class MetroidDoorBody extends AgentBody {
	private static final float BODY_WIDTH = UInfo.P2M(8f);
	private static final float BODY_HEIGHT = UInfo.P2M(48f);
	private static final CFBitSeq CFCAT_BITS =
			new CFBitSeq(CommonCF.Alias.AGENT_BIT, CommonCF.Alias.SOLID_BOUND_BIT);
	private static final CFBitSeq CFMASK_BITS = new CFBitSeq(true);

	private MetroidDoor parent;

	public MetroidDoorBody(MetroidDoor parent, World world, Vector2 position) {
		this.parent = parent;
		defineBody(world, position);
	}

	private void defineBody(World world, Vector2 position) {
		setBodySize(BODY_WIDTH, BODY_HEIGHT);
		// TODO: verify that catBits should be SOLID_BOUND_BIT, e.g.
		//   -will this interfere with on ground detection?
		//   -will zoomer be able to walk on door?
		//   -will this agent be confused with a solid bound line seg from collision map?
		b2body = B2DFactory.makeStaticBody(world, position);
		B2DFactory.makeBoxFixture(b2body, new FixtureDef(), this, CFCAT_BITS, CFMASK_BITS, BODY_WIDTH, BODY_HEIGHT);
	}

	public void enableRegularContacts() {
		for(Fixture fix : b2body.getFixtureList()) {
			if(!(fix.getUserData() instanceof AgentBodyFilter))
				continue;
			((AgentBodyFilter) fix.getUserData()).categoryBits = CFCAT_BITS;
			((AgentBodyFilter) fix.getUserData()).maskBits = CFMASK_BITS;
			// the contact filters were changed, so let Box2D know to update contacts here
			fix.refilter();
		}
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}