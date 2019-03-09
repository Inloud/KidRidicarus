package kidridicarus.common.agentbody.general;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agentbody.AgentBody;
import kidridicarus.agency.agentcontact.CFBitSeq;
import kidridicarus.common.agent.general.AgentSpawner;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.tool.B2DFactory;

public class AgentSpawnerBody extends AgentBody {
	private static final CFBitSeq CFCAT_BITS = new CFBitSeq(CommonCF.Alias.SPAWNBOX_BIT);
	private static final CFBitSeq CFMASK_BITS = new CFBitSeq(CommonCF.Alias.SPAWNTRIGGER_BIT);

	private AgentSpawner parent;

	public AgentSpawnerBody(AgentSpawner parent, World world, Rectangle bounds) {
		this.parent = parent;
		setBodySize(bounds.width, bounds.height);
		defineBody(world, bounds);
	}

	private void defineBody(World world, Rectangle bounds) {
		BodyDef bdef = new BodyDef();
		bdef.type = BodyDef.BodyType.StaticBody;
		bdef.position.set(bounds.getCenter(new Vector2()));
		FixtureDef fdef = new FixtureDef();
		fdef.isSensor = true;
		b2body = B2DFactory.makeSpecialBoxBody(world, bdef, fdef, this, CFCAT_BITS, CFMASK_BITS,
				bounds.width, bounds.height);
	}

	@Override
	public AgentSpawner getParent() {
		return parent;
	}
}
