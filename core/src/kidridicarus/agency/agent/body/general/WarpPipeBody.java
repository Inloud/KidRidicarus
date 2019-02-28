package kidridicarus.agency.agent.body.general;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agent.body.AgentBody;
import kidridicarus.agency.agent.general.WarpPipe;
import kidridicarus.agency.contact.CFBitSeq;
import kidridicarus.agency.tool.B2DFactory;
import kidridicarus.game.info.GameInfo;

public class WarpPipeBody extends AgentBody {
	private WarpPipe parent;

	public WarpPipeBody(WarpPipe parent, World world, Rectangle bounds) {
		this.parent = parent;
		defineBody(world, bounds);
	}

	private void defineBody(World world, Rectangle bounds) {
		setBodySize(bounds.width, bounds.height);
		CFBitSeq catBits = new CFBitSeq(GameInfo.CFBits.PIPE_BIT);
		CFBitSeq maskBits = new CFBitSeq(GameInfo.CFBits.AGENT_BIT);
		b2body = B2DFactory.makeBoxBody(world, BodyType.StaticBody, this, catBits, maskBits, bounds);
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}
