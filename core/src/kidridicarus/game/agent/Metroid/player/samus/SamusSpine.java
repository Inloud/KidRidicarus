package kidridicarus.game.agent.Metroid.player.samus;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.agent.Agent;
import kidridicarus.common.agent.roombox.RoomBox;
import kidridicarus.common.agentsensor.AgentContactHoldSensor;
import kidridicarus.common.agentsensor.SolidContactSensor;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.metaagent.tiledmap.collision.CollisionTiledMapAgent;
import kidridicarus.game.agent.SMB.TileBumpTakeAgent;
import kidridicarus.game.agent.SMB.other.bumptile.BumpTile.TileBumpStrength;
import kidridicarus.game.agentspine.SMB.PlayerSpine;

public class SamusSpine extends PlayerSpine {
	private static final float GROUNDMOVE_XIMP = 0.28f;
	private static final float MAX_GROUNDMOVE_VEL = 0.85f;
	private static final float MIN_WALK_VEL = 0.1f;
	private static final float STOPMOVE_XIMP = 0.15f;
	private static final float AIRMOVE_XIMP = GROUNDMOVE_XIMP * 0.7f;
	private static final float MAX_AIRMOVE_VEL = MAX_GROUNDMOVE_VEL;
	private static final float JUMPUP_FORCE = 8.33f;
	private static final float JUMPUP_CONSTVEL = 1f;
	private static final Vector2 DAMAGE_KICK_SIDE_IMP = new Vector2(1.8f, 0f);
	private static final Vector2 DAMAGE_KICK_UP_IMP = new Vector2(0f, 1.3f);
	private static final float MAX_UP_VELOCITY = 1.75f;
	private static final float MAX_DOWN_VELOCITY = 2.5f;
	private static final float HEADBOUNCE_VEL = 1.4f;	// up velocity
	private static final float MIN_HEADBANG_VEL = 0.01f;

	private AgentContactHoldSensor agentSensor;
	private SolidContactSensor sbSensor;
	private AgentContactHoldSensor tileBumpPushSensor;
	private float jumpStartY;

	public SamusSpine(SamusBody body) {
		super(body);
		agentSensor = null;
		sbSensor = null;
		tileBumpPushSensor = null;
		jumpStartY = 0;
	}

	public SolidContactSensor createSolidBodySensor() {
		sbSensor = new SolidContactSensor(body);
		return sbSensor;
	}

	public AgentContactHoldSensor creatAgentContactSensor() {
		agentSensor = new AgentContactHoldSensor(body);
		return agentSensor;
	}

	public AgentContactHoldSensor createTileBumpPushSensor() {
		tileBumpPushSensor = new AgentContactHoldSensor(body);
		return tileBumpPushSensor;
	}

	// apply walk impulse and cap horizontal velocity.
	public void applyWalkMove(boolean moveRight) {
		applyHorizImpulseAndCapVel(moveRight, GROUNDMOVE_XIMP, MAX_GROUNDMOVE_VEL);
	}

	// apply air impulse and cap horizontal velocity.
	public void applyAirMove(boolean moveRight) {
		applyHorizImpulseAndCapVel(moveRight, AIRMOVE_XIMP, MAX_AIRMOVE_VEL);
	}

	public void applyStopMove() {
		// if moving right...
		if(body.getVelocity().x > MIN_WALK_VEL)
			applyHorizontalImpulse(true, -STOPMOVE_XIMP);
		// if moving left...
		else if(body.getVelocity().x < -MIN_WALK_VEL)
			applyHorizontalImpulse(false, -STOPMOVE_XIMP);
		// not moving right or left fast enough, set horizontal velocity to zero to avoid wobbling
		else
			body.setVelocity(0f, body.getVelocity().y);
	}

	public void applyJumpForce(float forceTimer, float jumpForceDuration) {
		if(forceTimer < jumpForceDuration)
			body.applyForce(new Vector2(0f, JUMPUP_FORCE * forceTimer / jumpForceDuration));
	}

	public void setJumpStartPosition() {
		jumpStartY = body.getPosition().y;
	}

	public void applyDamageKick(Vector2 position) {
		// zero the y velocity
		body.setVelocity(body.getVelocity().x, 0);
		// apply a kick impulse to the left or right depending on other agent's position
		if(body.getPosition().x < position.x)
			body.applyImpulse(DAMAGE_KICK_SIDE_IMP.cpy().scl(-1f));
		else
			body.applyImpulse(DAMAGE_KICK_SIDE_IMP);

		// apply kick up impulse if the player is above the other agent
		if(body.getPosition().y > position.y)
			body.applyImpulse(DAMAGE_KICK_UP_IMP);
	}
	// jumpspin is allowed when body moves at least 2 tiles higher than jump start position 
	public boolean isJumpSpinAllowed() {
		return body.getPosition().y > jumpStartY + 2f*UInfo.P2M(UInfo.TILEPIX_Y);
	}

	public boolean isMapPointSolid(Vector2 position) {
		CollisionTiledMapAgent ctMap = agentSensor.getFirstContactByClass(CollisionTiledMapAgent.class);
		return ctMap == null ? false : ctMap.isMapPointSolid(position); 
	}

	public boolean isMapTileSolid(Vector2 tileCoords) {
		CollisionTiledMapAgent ctMap = agentSensor.getFirstContactByClass(CollisionTiledMapAgent.class);
		return ctMap == null ? false : ctMap.isMapTileSolid(tileCoords); 
	}

	public boolean isSolidOnThisSide(boolean isRightSide) {
		return sbSensor.isSolidOnThisSide(body.getBounds(), isRightSide);
	}

	public boolean isGiveHeadBounceAllowed(Rectangle otherBounds) {
		// check bounds
		Rectangle myBounds = body.getBounds();
		float otherCenterY = otherBounds.y+otherBounds.height/2f;
		if(myBounds.y >= otherCenterY || body.getPrevPosition().y-myBounds.height/2f >= otherCenterY)
			return true;
		return false;
	}

	public void doHeadBounce() {
		applyHeadBounceMove(HEADBOUNCE_VEL);
	}

	public RoomBox getCurrentRoom() {
		return (RoomBox) agentSensor.getFirstContactByClass(RoomBox.class);
	}

	public boolean isNoHorizontalVelocity() {
		return isStandingStill(MIN_WALK_VEL);
	}

	public void applyJumpVelocity() {
		body.setVelocity(body.getVelocity().x, JUMPUP_CONSTVEL);
	}

	public void doBounceCheck() {
		// Check for bounce up (no left/right bounces, no down bounces).
		// Since body restitution=0, bounce occurs when current velocity=0 and previous velocity > 0.
		// Check against 0 using velocity epsilon.
		if(UInfo.epsCheck(body.getVelocity().y, 0f, UInfo.VEL_EPSILON)) {
			float amount = -body.getPrevVelocity().y;
			if(amount > MAX_DOWN_VELOCITY-UInfo.VEL_EPSILON)
				amount = MAX_UP_VELOCITY-UInfo.VEL_EPSILON;
			else
				amount = amount * 0.6f;
			body.setVelocity(body.getVelocity().x, amount);
		}
	}

	/*
	 * If moving up fast enough, then check tiles currently contacting head for closest tile to take a bump.
	 * Tile bump is applied if needed.
	 * Returns true if tile bump is applied. Otherwise returns false.
	 */
	public boolean checkDoHeadBump(TileBumpStrength bumpStrength) {
		// exit if not moving up fast enough in this frame or previous frame
		if(body.getVelocity().y < MIN_HEADBANG_VEL || body.getPrevVelocity().y < MIN_HEADBANG_VEL)
			return false;
		// create list of bumptiles, in order from closest to player to farthest from player
		TreeSet<TileBumpTakeAgent> closestTilesList = 
				new TreeSet<TileBumpTakeAgent>(new Comparator<TileBumpTakeAgent>() {
				@Override
				public int compare(TileBumpTakeAgent o1, TileBumpTakeAgent o2) {
					float dist1 = Math.abs(((Agent) o1).getPosition().x - body.getPosition().x);
					float dist2 = Math.abs(((Agent) o2).getPosition().x - body.getPosition().x);
					if(dist1 < dist2)
						return -1;
					else if(dist1 > dist2)
						return 1;
					return 0;
				}
			});
		for(TileBumpTakeAgent bumpTile : tileBumpPushSensor.getContactsByClass(TileBumpTakeAgent.class))
			closestTilesList.add(bumpTile);

		// iterate through sorted list of bump tiles, exiting upon successful bump
		Iterator<TileBumpTakeAgent> tileIter = closestTilesList.iterator();
		while(tileIter.hasNext()) {
			TileBumpTakeAgent bumpTile = tileIter.next();
			// did the tile "take" the bump?
			if(bumpTile.onTakeTileBump(body.getParent(), bumpStrength))
				return true;
		}

		// no head bumps
		return false;
	}
}

/*
public class SamusSpine extends OnGroundSpine {
	private static final Vector2 DAMAGE_KICK_SIDE_IMP = new Vector2(1.8f, 0f);
	private static final Vector2 DAMAGE_KICK_UP_IMP = new Vector2(0f, 1.3f);

	private SamusBody body;
	private AgentContactHoldSensor acSensor;
	private SolidBoundSensor sbSensor;
	private AgentContactHoldSensor pwSensor;	// pipe warp sensor

	public SamusSpine(SamusBody body) {
		this.body = body;
		acSensor = null;
		sbSensor = null;
		pwSensor = null;
	}

	public SolidBoundSensor createSolidBodySensor() {
		sbSensor = new SolidBoundSensor(body);
		return sbSensor;
	}

	public AgentContactHoldSensor creatAgentContactSensor() {
		acSensor = new AgentContactHoldSensor(body);
		return acSensor;
	}

	public AgentContactHoldSensor createPipeWarpSensor() {
		pwSensor = new AgentContactHoldSensor(body);
		return pwSensor;
	}

	public void applyDamageKick(Vector2 position) {
		// zero the y velocity
		body.setVelocity(body.getVelocity().x, 0);
		// apply a kick impulse to the left or right depending on other agent's position
		if(body.getPosition().x < position.x)
			body.applyBodyImpulse(DAMAGE_KICK_SIDE_IMP.cpy().scl(-1f));
		else
			body.applyBodyImpulse(DAMAGE_KICK_SIDE_IMP);

		// apply kick up impulse if the player is above the other agent
		if(body.getPosition().y > position.y)
			body.applyBodyImpulse(DAMAGE_KICK_UP_IMP);
	}
*/
	/*
	 * Checks pipe warp sensors for a contacting pipe warp with entrance direction matching adviceDir, and
	 * returns pipe warp if found. Returns null otherwise. 
	 */
/*	public PipeWarp getPipeWarpForAdvice(Direction4 adviceDir) {
		for(PipeWarp pw : pwSensor.getContactsByClass(PipeWarp.class)) {
			if(((PipeWarp) pw).canBodyEnterPipe(body.getBounds(), adviceDir))
				return (PipeWarp) pw;
		}
		return null;
	}

	public <T> List<T> getContactsByClass(Class<T> cls) {
		return acSensor.getContactsByClass(cls);
	}

	public <T> T getFirstContactByClass(Class<T> cls) {
		return acSensor.getFirstContactByClass(cls);
	}

	public boolean isContactingWall(boolean isRightWall) {
		return sbSensor.isHMoveBlocked(body.getBounds(), isRightWall);
	}

	public RoomBox getCurrentRoom() {
		return (RoomBox) acSensor.getFirstContactByClass(RoomBox.class);
	}
}
*/