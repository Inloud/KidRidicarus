package kidridicarus.game.agent.SMB.item.fireflower;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agentbody.AgentBody;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.B2DFactory;

public class FireFlowerBody extends AgentBody {
	private static final float BODY_WIDTH = UInfo.P2M(14f);
	private static final float BODY_HEIGHT = UInfo.P2M(12f);

	private FireFlower parent;

	public FireFlowerBody(FireFlower parent, World world, Vector2 position) {
		this.parent = parent;
		defineBody(world, position);
	}

	private void defineBody(World world, Vector2 position) {
		setBodySize(BODY_WIDTH, BODY_HEIGHT);
		// items contact mario but can pass through goombas, turtles, etc.
		b2body = B2DFactory.makeDynamicBody(world, position);
		B2DFactory.makeBoxFixture(b2body, new FixtureDef(), this,
				CommonCF.SOLID_ITEM_CFCAT, CommonCF.SOLID_ITEM_CFMASK, BODY_WIDTH, BODY_HEIGHT);
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}