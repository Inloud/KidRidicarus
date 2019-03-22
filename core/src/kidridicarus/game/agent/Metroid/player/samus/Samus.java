package kidridicarus.game.agent.Metroid.player.samus;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency;
import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agent.AgentDrawListener;
import kidridicarus.agency.agent.AgentUpdateListener;
import kidridicarus.agency.agent.DisposableAgent;
import kidridicarus.agency.agentscript.ScriptedSpriteState;
import kidridicarus.agency.agentscript.ScriptedSpriteState.SpriteState;
import kidridicarus.agency.tool.AgencyDrawBatch;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.AgentSupervisor;
import kidridicarus.common.agent.optional.ContactDmgTakeAgent;
import kidridicarus.common.agent.optional.PlayerAgent;
import kidridicarus.common.agent.roombox.RoomBox;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.info.CommonKV;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.Direction4;
import kidridicarus.common.tool.MoveAdvice;
import kidridicarus.game.agent.Metroid.player.samusshot.SamusShot;
import kidridicarus.game.agent.SMB.HeadBounceGiveAgent;
import kidridicarus.game.agent.SMB.other.bumptile.BumpTile.TileBumpStrength;
import kidridicarus.game.agent.SMB.other.pipewarp.PipeWarp;
import kidridicarus.game.info.AudioInfo;

/*
 * TODO:
 * -samus loses JUMPSPIN when her y position goes below her jump start position
 */
public class Samus extends Agent implements PlayerAgent, ContactDmgTakeAgent, HeadBounceGiveAgent,
		DisposableAgent {
	public enum MoveState { STAND, BALL_GRND, RUN, RUNSHOOT, PRE_JUMPSHOOT, JUMPSHOOT, JUMPSPINSHOOT,
		PRE_JUMPSPIN, JUMPSPIN, PRE_JUMP, JUMP, BALL_AIR, CLIMB;
		public boolean equalsAny(MoveState ...otherStates) {
			for(MoveState state : otherStates) { if(this.equals(state)) return true; } return false;
		}
		public boolean isGroundMove() { return this.equalsAny(STAND, BALL_GRND, RUN, RUNSHOOT); }
		public boolean isRun() { return this.equalsAny(RUN, RUNSHOOT); }
		public boolean isJumpSpin() { return this.equalsAny(PRE_JUMPSPIN, JUMPSPIN, JUMPSPINSHOOT); }
		public boolean isBall() { return this.equalsAny(BALL_GRND, BALL_AIR); }
	}

	private static final float STEP_SOUND_TIME = 0.167f;
	private static final float POSTPONE_RUN_DELAY = 0.15f;
	private static final float JUMPUP_CONSTVEL_TIME = 0.017f;
	private static final float JUMPUP_FORCE_TIME = 0.75f;
	private static final float JUMPSHOOT_RESPIN_DELAY = 0.05f;
	private static final Vector2 SHOT_OFFSET_RIGHT = UInfo.P2MVector(11, 7);
	private static final Vector2 SHOT_OFFSET_UP = UInfo.P2MVector(1, 20);
	private static final float SHOT_VEL = 2f;
	private static final float SHOOT_COOLDOWN = 0.15f;
	private static final float NO_DAMAGE_TIME = 0.8f;

	private SamusSupervisor supervisor;
	private SamusBody body;
	private SamusSprite sprite;
	private MoveState moveState;
	private float moveStateTimer;
	private boolean isFacingRight;
	private boolean isFacingUp;
	private float noDamageCooldown;

	private float runStateTimer;
	private float lastStepSoundTime;
	private float jumpForceTimer;
	private boolean isNextJumpAllowed;
	private float shootCooldownTimer;
	private boolean didTakeDamage;
	private Vector2 takeDmgOrigin;
	private boolean gaveHeadBounce;
	private boolean isNextHeadBumpDenied;

	public Samus(Agency agency, ObjectProperties properties) {
		super(agency, properties);

		setStateFromProperties(properties);

		body = new SamusBody(this, agency.getWorld(), Agent.getStartPoint(properties));
		agency.addAgentUpdateListener(this, CommonInfo.AgentUpdateOrder.CONTACT_UPDATE, new AgentUpdateListener() {
			@Override
			public void update(float delta) { doContactUpdate(); }
		});
		agency.addAgentUpdateListener(this, CommonInfo.AgentUpdateOrder.UPDATE, new AgentUpdateListener() {
			@Override
			public void update(float delta) { doUpdate(delta); }
		});
		agency.addAgentUpdateListener(this, CommonInfo.AgentUpdateOrder.POST_UPDATE, new AgentUpdateListener() {
			@Override
			public void update(float delta) { doPostUpdate(); }
		});
		sprite = new SamusSprite(agency.getAtlas(), body.getPosition());
		agency.addAgentDrawListener(this, CommonInfo.LayerDrawOrder.SPRITE_TOP, new AgentDrawListener() {
			@Override
			public void draw(AgencyDrawBatch batch) { doDraw(batch); }
		});

		supervisor = new SamusSupervisor(agency, this, agency.getAtlas());
	}

	private void setStateFromProperties(ObjectProperties properties) {
		moveState = MoveState.STAND;
		moveStateTimer = 0f;
		isFacingRight = false;
		if(properties.get(CommonKV.KEY_DIRECTION, Direction4.NONE, Direction4.class) == Direction4.RIGHT)
			isFacingRight = true;
		isFacingUp = false;
		noDamageCooldown = 0f;

		runStateTimer = 0f;
		lastStepSoundTime = 0f;
		jumpForceTimer = 0f;
		// player must land on ground before first jump is allowed
		isNextJumpAllowed = false;
		shootCooldownTimer = 0f;
		didTakeDamage = false;
		takeDmgOrigin = new Vector2();
		gaveHeadBounce = false;
		isNextHeadBumpDenied = false;
	}

	/*
	 * Check for and do head bumps during contact update, so bump tiles can show results of bump immediately
	 * by way of regular update. Also apply star power damage if needed.
	 */
	private void doContactUpdate() {
		if(supervisor.isRunningScriptNoMoveAdvice())
			return;
		// exit if head bump flag hasn't reset
		if(isNextHeadBumpDenied)
			return;

		// if jump spinning then hit hard
		if(moveState.isJumpSpin())
			isNextHeadBumpDenied = body.getSpine().checkDoHeadBump(TileBumpStrength.HARD);
		else
			isNextHeadBumpDenied = body.getSpine().checkDoHeadBump(TileBumpStrength.SOFT);
	}

	private void doUpdate(float delta) {
		processContacts();
		processMove(delta, supervisor.pollMoveAdvice());
		processSprite(delta);
	}

	private void processContacts() {
		// if head bump deny flag is on, and body is moving down, then reset the flag
		if(isNextHeadBumpDenied && body.getSpine().isMovingDown())
			isNextHeadBumpDenied = false;
	}

	private void processMove(float delta, MoveAdvice moveAdvice) {
		if(!supervisor.isRunningScriptNoMoveAdvice()) {
			// since body has zero bounciness, a manual check is needed while in ball form 
			if(moveState.isBall())
				body.getSpine().doBounceCheck();
	
			processDamageTaken(delta);
			processHeadBouncesGiven();
			processPipeWarps(moveAdvice);
		}

		// if a script is running with no move advice then switch to scripted body state and exit
		if(supervisor.isRunningScriptNoMoveAdvice()) {
			body.useScriptedBodyState(supervisor.getScriptAgentState().scriptedBodyState);
			return;
		}

		MoveState nextMoveState = getNextMoveState(moveAdvice);
		if(nextMoveState.isGroundMove())
			processGroundMove(delta, moveAdvice, nextMoveState);
		else
			processAirMove(delta, moveAdvice, nextMoveState);

		moveStateTimer = moveState == nextMoveState ? moveStateTimer+delta : 0f;
		moveState = nextMoveState;

		processShoot(delta, moveAdvice);
	}

	private void processDamageTaken(float delta) {
		// if invulnerable to damage then exit
		if(noDamageCooldown > 0f) {
			noDamageCooldown -= delta;
			didTakeDamage = false;
			takeDmgOrigin.set(0f, 0f);
			return;
		}
		// if no damage taken this frame then exit
		else if(!didTakeDamage)
			return;
		// take damage
		didTakeDamage = false;
		noDamageCooldown = NO_DAMAGE_TIME;
		body.getSpine().applyDamageKick(takeDmgOrigin);
		takeDmgOrigin.set(0f, 0f);
		agency.getEar().onPlaySound(AudioInfo.Sound.Metroid.HURT);
	}

	private void processHeadBouncesGiven() {
		// if a head bounce was given in the update frame then reset the flag and do bounce move
		if(gaveHeadBounce) {
			gaveHeadBounce = false;
			body.getSpine().doHeadBounce();
		}
	}

	private void processPipeWarps(MoveAdvice moveAdvice) {
		PipeWarp pw = body.getSpine().getEnterPipeWarp(moveAdvice.getMoveDir4());
		if(pw != null)
			pw.use(this);
	}

	private MoveState getNextMoveState(MoveAdvice moveAdvice) {
		// if [on ground flag is true] and agent isn't [moving upward while in air move state], then do ground move
		if(body.getSpine().isOnGround() && !(body.getSpine().isMovingUp() && !moveState.isGroundMove()))
			return getNextMoveStateGround(moveAdvice);
		// do air move
		else
			return getNextMoveStateAir(moveAdvice);
	}

	private MoveState getNextMoveStateGround(MoveAdvice moveAdvice) {
		if(moveState.isBall()) {
			// if advised move up and not move down, and map tile above player is not solid, then change to stand
			if(moveAdvice.moveUp && !moveAdvice.moveDown &&
					!body.getSpine().isMapTileSolid(UInfo.getM2PTileForPos(body.getPosition()).add(0, 1)))
				return MoveState.STAND;
			// continue current ball state
			else
				return moveState;
		}
		// if already standing and advised to move down but not advised to move horizontally then start ball state
		else if(moveState == MoveState.STAND && moveAdvice.moveDown &&
				!moveAdvice.moveRight && !moveAdvice.moveLeft)
			return MoveState.BALL_GRND;
		// if advised to jump and allowed to jump then...
		else if(moveAdvice.action1 && isNextJumpAllowed) {
			// if advised move right or left but not both at same time then do jump spin
			if(moveAdvice.moveRight^moveAdvice.moveLeft && !moveAdvice.action0)
				return MoveState.PRE_JUMPSPIN;
			// if advised shoot then do jumpshoot
			else if(moveAdvice.action0)
				return MoveState.PRE_JUMPSHOOT;
			else
				return MoveState.PRE_JUMP;
		}
		else if(body.getSpine().isNoHorizontalVelocity()) {
			// If advised move right and contacting right wall, or
			// if advised move left and contacting left wall, or
			// if no left/right move advice,
			// then stand.
			if((moveAdvice.moveRight && body.getSpine().isSolidOnThisSide(true)) ||
					(moveAdvice.moveLeft && body.getSpine().isSolidOnThisSide(false)) ||
					!(moveAdvice.moveRight^moveAdvice.moveLeft))
				return MoveState.STAND;
			else {
				if(moveAdvice.action0)
					return MoveState.RUNSHOOT;
				else
					return MoveState.RUN;
			}
		}
		else if(moveAdvice.action0 || (moveState == MoveState.RUNSHOOT && moveStateTimer < POSTPONE_RUN_DELAY))
			return MoveState.RUNSHOOT;
		else
			return MoveState.RUN;
	}

	private MoveState getNextMoveStateAir(MoveAdvice moveAdvice) {
		if(moveState.isBall()) {
			// If advised to move up and not move down and tile above samus head is not solid then change to
			// standing type state.
			if(moveAdvice.moveUp && !moveAdvice.moveDown &&
					!body.getSpine().isMapTileSolid(UInfo.getM2PTileForPos(body.getPosition()).add(0, 1)))
				return MoveState.JUMP;
			// continue air ball state if already in air ball state, or change from ground to air ball state
			else
				return MoveState.BALL_AIR;
		}
		else if(moveState == MoveState.JUMPSHOOT)
			return MoveState.JUMPSHOOT;
		else if(moveState == MoveState.JUMPSPINSHOOT) {
			// if not moving up then lose spin and do jumpshoot
			if(!body.getSpine().isMovingUp())
				return MoveState.JUMPSHOOT;
			// If not advised to shoot, and advised move left or right but not both, and respin is allowed,
			// then do jumpspin.
			else if(!moveAdvice.action0 && moveAdvice.moveRight^moveAdvice.moveLeft &&
					moveStateTimer > JUMPSHOOT_RESPIN_DELAY)
				return MoveState.JUMPSPIN;
			// continue jumpshoot
			else
				return MoveState.JUMPSPINSHOOT;
		}
		else if(moveState == MoveState.JUMPSPIN) {
			// if advised to shoot then switch to jumpspin with shoot
			if(moveAdvice.action0)
				return MoveState.JUMPSPINSHOOT;
			// continue jumpspin
			else
				return MoveState.JUMPSPIN;
		}
		else if(moveState == MoveState.PRE_JUMPSPIN) {
			// is change to jumpspin allowed?
			if(moveStateTimer > JUMPUP_CONSTVEL_TIME) {
				// if shooting then enter jumpspin with shoot
				if(moveAdvice.action0)
					return MoveState.JUMPSPINSHOOT;
				// start jumpspin
				else
					return MoveState.JUMPSPIN;
			}
			// continue pre-jumpspin
			else
				return MoveState.PRE_JUMPSPIN;
		}
		else if(moveState == MoveState.PRE_JUMP) {
			if(moveStateTimer > JUMPUP_CONSTVEL_TIME)
				return MoveState.JUMP;
			else
				return MoveState.PRE_JUMP;
		}
		else
			return MoveState.JUMP;
	}

	private void processGroundMove(float delta, MoveAdvice moveAdvice, MoveState nextMoveState) {
		// next jump changes to allowed when on ground and not advising jump move 
		if(!moveAdvice.action1)
			isNextJumpAllowed = true;

		boolean applyWalkMove = false;
		boolean applyStopMove = false;
		// if advised to move right or move left, but not both at same time, then...
		if(moveAdvice.moveRight^moveAdvice.moveLeft) {
			if(isFacingRight && moveAdvice.moveLeft)
				isFacingRight = false;
			else if(!isFacingRight && moveAdvice.moveRight)
				isFacingRight = true;

			applyWalkMove = true;
		}
		else
			applyStopMove = true;

		// if advised to move up or move down, but not both at same time, then...
		if(moveAdvice.moveUp^moveAdvice.moveDown)
			isFacingUp = moveAdvice.moveUp;
		else
			isFacingUp = false;

		// if previous move air move then samus just landed, so play landing sound
		if(!moveState.isGroundMove())
			agency.getEar().onPlaySound(AudioInfo.Sound.Metroid.STEP);
		// if last move and this move are run moves then check/do step sound
		else if(moveState.isRun() && nextMoveState.isRun()) {
			if(runStateTimer - lastStepSoundTime >= STEP_SOUND_TIME) {
				lastStepSoundTime = runStateTimer;
				agency.getEar().onPlaySound(AudioInfo.Sound.Metroid.STEP);
			}
		}
		else
			lastStepSoundTime = 0f;

		// if changed to ball state from non-ball state then decrease body size
		if(nextMoveState.isBall() && !moveState.isBall())
			body.setBallForm(true);
		// if changed to non-ball state from ball state then increase body size 
		if(!nextMoveState.isBall() && moveState.isBall())
			body.setBallForm(false);

		switch(nextMoveState) {
			case STAND:
			case RUN:
			case RUNSHOOT:
			case BALL_GRND:
				break;
			default:
				throw new IllegalStateException("Wrong ground nextMoveState = " + nextMoveState);
		}

		if(applyWalkMove)
			body.getSpine().applyWalkMove(isFacingRight);
		if(applyStopMove)
			body.getSpine().applyStopMove();

		runStateTimer = nextMoveState.isRun() ? runStateTimer+delta : 0f;
	}

	private void processAirMove(float delta, MoveAdvice moveAdvice, MoveState nextMoveState) {
		// check for change of facing direction
		if(moveAdvice.moveRight^moveAdvice.moveLeft) {
			if(isFacingRight && moveAdvice.moveLeft)
				isFacingRight = false;
			else if(!isFacingRight && moveAdvice.moveRight)
				isFacingRight = true;
		}

		// facing up can change during jumpspin while player moves upward
		if(nextMoveState.isJumpSpin() && body.getSpine().isMovingUp()) {
			if(moveAdvice.moveUp && !moveAdvice.moveDown)
				isFacingUp = true;
			else
				isFacingUp = false;
		}

		// if changed to non-ball state from ball state then increase body size 
		if(!nextMoveState.isBall() && moveState.isBall())
			body.setBallForm(false);
		// note: cannot change from non-ball state to ball state while mid-air

		switch(nextMoveState) {
			case PRE_JUMPSPIN:
			case PRE_JUMP:
			case PRE_JUMPSHOOT:
				// if previously on ground then...
				if(moveState.isGroundMove()) {
					// if advised jump and allowed to jump then do it 
					if(moveAdvice.action1 && isNextJumpAllowed) {
						isNextJumpAllowed = false;
						jumpForceTimer = JUMPUP_CONSTVEL_TIME+JUMPUP_FORCE_TIME;
						agency.getEar().onPlaySound(AudioInfo.Sound.Metroid.JUMP);
					}
				}
				body.getSpine().applyJumpVelocity();
				break;
			case JUMP:
			case JUMPSPIN:
			case JUMPSHOOT:
			case JUMPSPINSHOOT:
				// This takes care of player falling off ledge - player cannot jump again until at least
				// one frame has elapsed where player was on ground and not advised to  jump (to prevent
				// re-jump bouncing).
				isNextJumpAllowed = false;

				// if jump force continues and jump is advised then do jump force
				if(jumpForceTimer > 0f && moveAdvice.action1)
					body.getSpine().applyJumpForce(jumpForceTimer-JUMPUP_CONSTVEL_TIME, JUMPUP_FORCE_TIME);

				break;
			case BALL_AIR:
				break;
			default:
				throw new IllegalStateException("Wrong air nextMoveState = " + nextMoveState);
		}

		// disallow jump force until next jump when [jump advice stops] or [body stops moving up]
		if(!moveAdvice.action1 || !body.getSpine().isMovingUp())
			jumpForceTimer = 0f;

		// if advised move right or move left but not both at same time then...
		if(moveAdvice.moveRight^moveAdvice.moveLeft) {
			// if advised move right and facing left then change to facing right
			if(moveAdvice.moveRight && !isFacingRight)
				isFacingRight = true;
			// if advised move left and facing right then change to facing left
			else if(moveAdvice.moveLeft && isFacingRight)
				isFacingRight = false;

			body.getSpine().applyAirMove(isFacingRight);
		}
		// if in jumpspin then maintain velocity, otherwise apply stop move 
		else if(moveState != MoveState.JUMPSPIN)
			body.getSpine().applyStopMove();

		// decrement jump force timer
		jumpForceTimer = jumpForceTimer > delta ? jumpForceTimer-delta : 0f;
	}

	private void processShoot(float delta, MoveAdvice moveAdvice) {
		if(moveAdvice.action0 && isShootAllowed() && !moveState.isBall())
			doShoot();
		// decrememnt shoot cooldown timer
		shootCooldownTimer = shootCooldownTimer > delta ? shootCooldownTimer-delta : 0f;
	}

	private boolean isShootAllowed() {
		return shootCooldownTimer <= 0f;
	}

	private void doShoot() {
		shootCooldownTimer = SHOOT_COOLDOWN;

		// calculate position and velocity of shot based on samus' orientation
		Vector2 velocity = new Vector2();
		Vector2 position = new Vector2();
		if(isFacingUp) {
			velocity.set(0f, SHOT_VEL);
			if(isFacingRight)
				position.set(SHOT_OFFSET_UP).add(body.getPosition());
			else
				position.set(SHOT_OFFSET_UP).scl(-1, 1).add(body.getPosition());
		}
		else if(isFacingRight) {
			velocity.set(SHOT_VEL, 0f);
			position.set(SHOT_OFFSET_RIGHT).add(body.getPosition());
		}
		else {
			velocity.set(-SHOT_VEL, 0f);
			position.set(SHOT_OFFSET_RIGHT).scl(-1, 1).add(body.getPosition());
		}

		// create shot
		ObjectProperties shotProps = SamusShot.makeAP(this, position, velocity);
		// if the spawn point of shot is in a solid tile then the shot must immediately explode
		if(body.getSpine().isMapPointSolid(position))
			shotProps.put(CommonKV.Spawn.KEY_EXPIRE, true);
		agency.createAgent(shotProps);
		agency.getEar().onPlaySound(AudioInfo.Sound.Metroid.SHOOT);
	}

	private void doPostUpdate() {
		// let body update previous position/velocity
		body.postUpdate();
	}

	private void processSprite(float delta) {
		if(supervisor.isRunningScriptNoMoveAdvice()) {
			MoveState scriptedMoveState;
			ScriptedSpriteState sss = supervisor.getScriptAgentState().scriptedSpriteState;
			switch(sss.spriteState) {
				case MOVE:
					scriptedMoveState = MoveState.RUN;
					break;
				case CLIMB:
					scriptedMoveState = MoveState.CLIMB;
					break;
				case STAND:
				default:
					scriptedMoveState = MoveState.STAND;
					break;
			}
			sprite.update(delta, sss.position, scriptedMoveState, sss.isFacingRight, false, false, sss.moveDir);
		}
		else {
			sprite.update(delta, body.getPosition(), moveState, isFacingRight, isFacingUp,
					(noDamageCooldown > 0f), Direction4.NONE);
		}
	}

	private void doDraw(AgencyDrawBatch batch) {
		// exit if using scripted sprite state and script says don't draw
		if(supervisor.isRunningScriptNoMoveAdvice() &&
				!supervisor.getScriptAgentState().scriptedSpriteState.visible)
			return;
		batch.draw(sprite);
	}

	@Override
	public boolean onTakeDamage(Agent agent, float amount, Vector2 dmgOrigin) {
		// no damage taken if already took damage this frame, or if invulnerable, or if star powered, or if dead
		if(didTakeDamage || noDamageCooldown > 0f)
			return false;
		didTakeDamage = true;
		takeDmgOrigin.set(dmgOrigin);
		return true;
	}

	@Override
	public boolean onGiveHeadBounce(Agent agent) {
		// if head bounce is allowed then give head bounce to agent
		if(body.getSpine().isGiveHeadBounceAllowed(agent.getBounds())) {
			gaveHeadBounce = true;
			return true;
		}
		return false;
	}

	@Override
	public AgentSupervisor getSupervisor() {
		return supervisor;
	}

	@Override
	public RoomBox getCurrentRoom() {
		return body.getSpine().getCurrentRoom();
	}

	@Override
	public Vector2 getPosition() {
		return body.getPosition();
	}

	@Override
	public Rectangle getBounds() {
		return body.getBounds();
	}

	// unchecked cast to T warnings ignored because T is checked with class.equals(cls) 
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getProperty(String key, Object defaultValue, Class<T> cls) {
		if(key.equals(CommonKV.Script.KEY_FACINGRIGHT) && Boolean.class.equals(cls)) {
			Boolean he = isFacingRight;
			return (T) he;
		}
		else if(key.equals(CommonKV.Script.KEY_SPRITESTATE) && SpriteState.class.equals(cls)) {
			SpriteState he = SpriteState.STAND;
			return (T) he;
		}
		else if(key.equals(CommonKV.Script.KEY_SPRITESIZE) && Vector2.class.equals(cls)) {
			Vector2 he = new Vector2(sprite.getWidth(), sprite.getHeight());
			return (T) he;
		}
		return super.getProperty(key, defaultValue, cls);
	}

	@Override
	public ObjectProperties getCopyAllProperties() {
		ObjectProperties op = properties.cpy();
		op.put(CommonKV.Script.KEY_FACINGRIGHT, isFacingRight);
		return op;
	}

	@Override
	public void disposeAgent() {
		body.dispose();
	}
}
