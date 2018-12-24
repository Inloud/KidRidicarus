package kidridicarus.agent.bodies.SMB;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.B2DFactory;
import kidridicarus.agency.contacts.CFBitSeq;
import kidridicarus.agency.contacts.CFBitSeq.CFBit;
import kidridicarus.agent.Agent;
import kidridicarus.agent.SMB.PipeWarp;
import kidridicarus.agent.bodies.AgentBody;

public class PipeWarpBody extends AgentBody {
	private PipeWarp parent;

	public PipeWarpBody(PipeWarp parent, World world, Rectangle bounds) {
		this.parent = parent;
		defineBody(world, bounds);
	}

	private void defineBody(World world, Rectangle bounds) {
		setBodySize(bounds.width, bounds.height);
//		b2body = B2DFactory.makeBoxBody(world, BodyType.StaticBody, this, GameInfo.PIPE_BIT,
//				(short) (GameInfo.GUIDE_SENSOR_BIT | GameInfo.GUIDE_SENSOR_BIT), bounds);
		CFBitSeq catBits = new CFBitSeq(CFBit.PIPE_BIT);
		CFBitSeq maskBits = new CFBitSeq(CFBit.GUIDE_SENSOR_BIT);
		b2body = B2DFactory.makeBoxBody(world, BodyType.StaticBody, this, catBits, maskBits, bounds);
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}
