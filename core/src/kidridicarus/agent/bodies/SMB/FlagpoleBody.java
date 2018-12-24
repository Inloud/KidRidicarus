package kidridicarus.agent.bodies.SMB;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.B2DFactory;
import kidridicarus.agency.contacts.CFBitSeq;
import kidridicarus.agency.contacts.CFBitSeq.CFBit;
import kidridicarus.agent.Agent;
import kidridicarus.agent.SMB.Flagpole;
import kidridicarus.agent.bodies.AgentBody;

public class FlagpoleBody extends AgentBody {
	private Flagpole parent;

	public FlagpoleBody(Flagpole parent, World world, Rectangle bounds) {
		this.parent = parent;
		defineBody(world, bounds);
	}

	private void defineBody(World world, Rectangle bounds) {
		setBodySize(bounds.width, bounds.height);
//		b2body = B2DFactory.makeBoxBody(world, BodyType.StaticBody, this, GameInfo.AGENT_BIT,
//				(short) (GameInfo.GUIDE_SENSOR_BIT | GameInfo.AGENT_BIT), bounds);
		CFBitSeq catBits = new CFBitSeq(CFBit.AGENT_BIT);
		CFBitSeq maskBits = new CFBitSeq(CFBit.GUIDE_SENSOR_BIT, CFBit.AGENT_BIT);
		b2body = B2DFactory.makeBoxBody(world, BodyType.StaticBody, this, catBits, maskBits, bounds);
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}
