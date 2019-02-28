package kidridicarus.game.SMB.agentbody.NPC;

import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.contact.AgentBodyFilter;
import kidridicarus.agency.contact.CFBitSeq;
import kidridicarus.agency.info.UInfo;
import kidridicarus.agency.tool.B2DFactory;
import kidridicarus.common.agentbody.MobileAgentBody;
import kidridicarus.common.agentbody.sensor.AgentContactBeginSensor;
import kidridicarus.common.agentbody.sensor.AgentContactSensor;
import kidridicarus.common.agentbody.sensor.OnGroundSensor;
import kidridicarus.common.agentbody.sensor.SolidBoundSensor;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.game.SMB.agent.NPC.Turtle;
import kidridicarus.game.SMB.agentbody.BumpableBody;

public class TurtleBody extends MobileAgentBody implements BumpableBody {
	private static final float BODY_WIDTH = UInfo.P2M(14f);
	private static final float BODY_HEIGHT = UInfo.P2M(14f);
	private static final float FOOT_WIDTH = UInfo.P2M(12f);
	private static final float FOOT_HEIGHT = UInfo.P2M(4f);

	private Turtle parent;
	private OnGroundSensor ogSensor;
	private SolidBoundSensor hmSensor;
	private AgentContactSensor acSensor;
	private AgentContactBeginSensor kickSensor;

	public TurtleBody(Turtle parent, World world, Vector2 position) {
		this.parent = parent;
		defineBody(world, position);
	}

	private void defineBody(World world, Vector2 position) {
		setBodySize(BODY_WIDTH, BODY_HEIGHT);
		createSolidBody(world, position);
		createAgentSensor();
		createGroundSensor();
	}

	private void createSolidBody(World world, Vector2 position) {
		CFBitSeq catBits = new CFBitSeq(CommonInfo.CFBits.AGENT_BIT);
		CFBitSeq maskBits = new CFBitSeq(CommonInfo.CFBits.SOLID_BOUND_BIT);
		hmSensor = new SolidBoundSensor(parent);
		b2body = B2DFactory.makeBoxBody(world, BodyType.DynamicBody, hmSensor, catBits, maskBits, position,
				BODY_WIDTH, BODY_HEIGHT);
	}

	private void createAgentSensor() {
		FixtureDef fdef = new FixtureDef();
		PolygonShape boxShape = new PolygonShape();
		boxShape.setAsBox(BODY_WIDTH/2f, BODY_HEIGHT/2f);
		fdef.isSensor = true;
		fdef.shape = boxShape;
		CFBitSeq catBits = new CFBitSeq(CommonInfo.CFBits.AGENT_BIT);
		CFBitSeq maskBits = new CFBitSeq(CommonInfo.CFBits.AGENT_BIT);
		acSensor = new AgentContactSensor(this);
		kickSensor = new AgentContactBeginSensor(this);
		kickSensor.chainTo(acSensor);
		b2body.createFixture(fdef).setUserData(new AgentBodyFilter(catBits, maskBits, kickSensor));
	}

	private void createGroundSensor() {
		FixtureDef fdef = new FixtureDef();
		PolygonShape boxShape = new PolygonShape();
		boxShape.setAsBox(FOOT_WIDTH/2f, FOOT_HEIGHT/2f, new Vector2(0f, -BODY_HEIGHT/2f), 0f);
		fdef.isSensor = true;
		fdef.shape = boxShape;
		CFBitSeq catBits = new CFBitSeq(CommonInfo.CFBits.AGENT_BIT);
		CFBitSeq maskBits = new CFBitSeq(CommonInfo.CFBits.SOLID_BOUND_BIT);
		ogSensor = new OnGroundSensor(null);
		b2body.createFixture(fdef).setUserData(new AgentBodyFilter(catBits, maskBits, ogSensor));
	}

	@Override
	public void onBump(Agent bumpingAgent) {
		parent.onBump(bumpingAgent);
	}

	public boolean isMoveBlocked(boolean moveRight) {
		return hmSensor.isHMoveBlocked(getBounds(), moveRight);
	}

	public boolean isMoveBlockedByAgent(boolean moveRight) {
		return AgentContactSensor.isMoveBlockedByAgent(acSensor, getPosition(), moveRight);
	}

	public boolean isOnGround() {
		// return true if the on ground contacts list contains at least 1 floor
		return ogSensor.isOnGround();
	}

	public List<Agent> getAndResetContactBeginAgents() {
		return kickSensor.getAndResetContacts();
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}