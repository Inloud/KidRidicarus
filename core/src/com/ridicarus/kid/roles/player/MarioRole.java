package com.ridicarus.kid.roles.player;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import com.ridicarus.kid.GameInfo;
import com.ridicarus.kid.collisionmap.LineSeg;
import com.ridicarus.kid.roles.PlayerRole;
import com.ridicarus.kid.roles.RobotRole;
import com.ridicarus.kid.roles.robot.DamageableBot;
import com.ridicarus.kid.roles.robot.Flagpole;
import com.ridicarus.kid.roles.robot.HeadBounceBot;
import com.ridicarus.kid.roles.robot.ItemRobot;
import com.ridicarus.kid.roles.robot.ItemRobot.PowerupType;
import com.ridicarus.kid.roles.robot.MarioFireball;
import com.ridicarus.kid.roles.robot.TouchDmgBot;
import com.ridicarus.kid.roles.robot.Turtle;
import com.ridicarus.kid.sprites.MarioSprite;
import com.ridicarus.kid.tiles.InteractiveTileObject;
import com.ridicarus.kid.tools.WorldRunner;

/*
 * TODO:
 * -the body physics code has only been tested with non-moving surfaces, needs to be tested with moving platforms
 */
public class MarioRole implements PlayerRole {
	private static final float MARIO_WALKMOVE_XIMP = 0.025f;
	private static final float MARIO_MIN_WALKSPEED = MARIO_WALKMOVE_XIMP * 2;
	private static final float MARIO_RUNMOVE_XIMP = MARIO_WALKMOVE_XIMP * 1.5f;
	private static final float DECEL_XIMP = MARIO_WALKMOVE_XIMP * 1.37f;
	private static final float MARIO_BRAKE_XIMP = MARIO_WALKMOVE_XIMP * 2.75f;
	private static final float MARIO_BRAKE_TIME = 0.2f;
	private static final float MARIO_MAX_WALKVEL = MARIO_WALKMOVE_XIMP * 42f;
	private static final float MARIO_MAX_RUNVEL = MARIO_MAX_WALKVEL * 1.65f;

	private static final float MARIO_JUMP_IMPULSE = 1.75f;
	private static final float MARIO_JUMP_FORCE = 14f;
	private static final float MARIO_AIRMOVE_XIMP = 0.04f;
	private static final float MARIO_RUNJUMP_MULT = 0.25f;
	private static final float MARIO_MAX_RUNJUMPVEL = MARIO_MAX_RUNVEL;
	private static final float MARIO_JUMP_GROUNDCHECK_DELAY = 0.05f;
	private static final float MARIO_JUMPFORCE_TIME = 0.5f;
	private static final float MARIO_HEADBOUNCE_VEL = 1.75f;	// up velocity

	private static final float DMG_INVINCIBLE_TIME = 3f;
	private static final float FIREBALL_OFFSET = GameInfo.P2M(8f);
	private static final float TIME_PER_FIREBALL = 0.5f;
	private static final float POWERSTAR_TIME = 15f;
	private static final float FLAG_SLIDE_VELOCITY = -0.9f;
	private static final float END_FLAGWAIT = 0.4f;
	private static final float END_BRAKETIME = 0.02f;
	private static final Vector2 FLAG_JUMPOFF_VEL = new Vector2(1.0f, 1.0f);

	public enum MarioPowerState { SMALL, BIG, FIRE };
	// the body may receive different impulses than the sprite receives texture regions
	public enum MarioCharState { STAND, WALKRUN, BRAKE, JUMP, FALL, DUCK, FIREBALL, DEAD, END1_SLIDE, END2_WAIT1, END3_WAIT2, END4_FALL, END5_BRAKE, END6_RUN };

	private WorldRunner runner;
	private MarioSprite marioSprite;
	private Body b2body;
	private Fixture marioBodyFixture;	// for making mario invincible after damage

	private MarioCharState curCharState;
	private MarioPowerState curPowerState;
	private float stateTimer;

	private boolean wantsToGoRight;
	private boolean wantsToGoLeft;
	private boolean wantsToGoDown;
	private boolean wantsToJump;
	private boolean wantsToRun;

	private boolean marioIsDead;
	private boolean isFacingRight;

	private int onGroundCount;
	private boolean isOnGround;
	private boolean isNewJumpAllowed;

	private boolean isDmgInvincible;
	private float dmgInvincibleTime;

	private boolean canHeadBang;
	private Array<InteractiveTileObject> headHits;

	private boolean isTakeDamage;
	private boolean isHeadBouncing;
	private PowerupType receivedPowerup;

	private boolean wantsToRunOnPrevUpdate;
	private boolean isBrakeAvailable;
	private float brakeTimer;
	private float jumpGroundCheckTimer;
	private boolean isJumping;
	private float jumpForceTimer;
	private float fireballTimer;
	private float powerStarTimer;
	private boolean isDucking;
	private float bodyFakeHeight;
	private Flagpole touchedFlagpole;
	private boolean isLevelEnd;

	public MarioRole(WorldRunner runner, Vector2 position) {
		this.runner = runner;

		marioIsDead = false;
		isFacingRight = true;
		onGroundCount = 0;
		isOnGround = false;
		isNewJumpAllowed = false;
		isDmgInvincible = false;
		dmgInvincibleTime = 0f;
		canHeadBang = true;
		headHits = new Array<InteractiveTileObject>();

		curCharState = MarioCharState.STAND;
		stateTimer = 0f;
		curPowerState = MarioPowerState.SMALL;

		isTakeDamage = false;
		isHeadBouncing = false;
		receivedPowerup = PowerupType.NONE;

		fireballTimer = TIME_PER_FIREBALL * 2f;
		wantsToRunOnPrevUpdate = false;

		isBrakeAvailable = true;
		brakeTimer = 0f;
		jumpGroundCheckTimer = 0f;
		isJumping = false;
		jumpForceTimer = 0f;
		powerStarTimer = 0f;
		isDucking = false;
		touchedFlagpole = null;
		isLevelEnd = false;

		// graphic
		marioSprite = new MarioSprite(runner.getAtlas(), position, curCharState, curPowerState, isFacingRight);
		// physic
		defineBody(position, new Vector2(0f, 0f));
	}

	// Process the body and return a character state based on the findings.
	private MarioCharState processBodyState(float delta) {
		MarioCharState returnState;
		boolean isVelocityLeft, isVelocityRight;
		boolean doWalkRunMove;
		boolean doDecelMove;
		boolean doBrakeMove;

		if(marioIsDead) {
			// make sure mario doesn't move left or right while dead
			b2body.setLinearVelocity(0f, b2body.getLinearVelocity().y);
			return MarioCharState.DEAD;
		}
		// scripted level end sequence using curCharState and stateTimer
		else if(isLevelEnd) {
			switch(curCharState) {
				case END1_SLIDE:
					// switch sides if necessary when hit ground
					if(isOnGround)
						return MarioCharState.END2_WAIT1;
					// sliding down
					else {
						b2body.setLinearVelocity(0f, FLAG_SLIDE_VELOCITY);
						return MarioCharState.END1_SLIDE;
					}
				case END2_WAIT1:
					if(touchedFlagpole.isAtBottom()) {
						isFacingRight = false;
						// if mario is on left side of flagpole, move him to right side
						if(touchedFlagpole.getBody().getPosition().x > b2body.getPosition().x)
							defineBody(b2body.getPosition().cpy().add(
									2f * (touchedFlagpole.getBody().getPosition().x - b2body.getPosition().x), 0f),
									new Vector2(0f, 0f));
						return MarioCharState.END3_WAIT2;
					}
					else
						return MarioCharState.END2_WAIT1;
				case END3_WAIT2:
					if(stateTimer > END_FLAGWAIT) {
						// switch to first walk frame and push mario to right
						b2body.setGravityScale(1f);
						b2body.applyLinearImpulse(FLAG_JUMPOFF_VEL, b2body.getWorldCenter(), true);
						return MarioCharState.END4_FALL;
					}
					else
						return MarioCharState.END3_WAIT2;
				case END4_FALL:
					if(isOnGround)
						return MarioCharState.END5_BRAKE;
					else
						return MarioCharState.END4_FALL;
				case END5_BRAKE:
					if(stateTimer > END_BRAKETIME) {
						isFacingRight = true;
						runner.startMusic(GameInfo.MUSIC_LEVELEND, false);
						touchedFlagpole = null;
						return MarioCharState.END6_RUN;
					}
					else
						return MarioCharState.END5_BRAKE;
				case END6_RUN:
 					moveBodyLeftRight(true, false);
					return MarioCharState.END6_RUN;
				// first level end state
				default:
					touchedFlagpole.startDrop();
					b2body.setGravityScale(0f);
					b2body.setLinearVelocity(0f, 0f);

					runner.stopMusic();
					runner.playSound(GameInfo.SOUND_FLAGPOLE);

					return MarioCharState.END1_SLIDE;
			}
		}

		returnState = MarioCharState.STAND;
		isVelocityRight = b2body.getLinearVelocity().x > MARIO_MIN_WALKSPEED;
		isVelocityLeft = b2body.getLinearVelocity().x < -MARIO_MIN_WALKSPEED;

		// if mario's velocity is below walking speed then set it to 0
		if(isOnGround && !isVelocityRight && !isVelocityLeft && !wantsToGoRight && !wantsToGoLeft)
			b2body.setLinearVelocity(0f, b2body.getLinearVelocity().y);

		// multiple concurrent body impulses may be necessary
		doWalkRunMove = false;
		doDecelMove = false;
		doBrakeMove = false;

		// eligible for duck/unduck?
		if(curPowerState != MarioPowerState.SMALL && isOnGround) {
			// first time duck check
			if(wantsToGoDown && !isDucking) {
				// quack
				isDucking = true;
				defineBody(b2body.getPosition().cpy().sub(0f, GameInfo.P2M(8f)), b2body.getLinearVelocity());
			}
			// first time unduck check
			else if(!wantsToGoDown && isDucking) {
				// kcauq
				isDucking = false;
				defineBody(b2body.getPosition().cpy().add(0f, GameInfo.P2M(8f)), b2body.getLinearVelocity());
			}
		}

		// want to move left or right? (but not both! because they would cancel each other)
		if((wantsToGoRight && !wantsToGoLeft) || (!wantsToGoRight && wantsToGoLeft)) {
			doWalkRunMove = true;

			// mario can change facing direction, but not while airborne
			if(isOnGround) {
				// brake becomes available again when facing direction changes
				if(isFacingRight != wantsToGoRight) {
					isBrakeAvailable = true;
					brakeTimer = 0f;
				}

				// can't run/walk on ground while ducking, only slide
				if(isDucking) {
					doWalkRunMove = false;
					doDecelMove = true;
				}
				else	// update facing direction
					isFacingRight = wantsToGoRight;
			}
		}
		// decelerate if on ground and not wanting to move left or right
		else if(isOnGround && (isVelocityRight || isVelocityLeft))
			doDecelMove = true;

		// check for brake application
		if(!isDucking && isOnGround && isBrakeAvailable && ((isFacingRight && isVelocityLeft) || (!isFacingRight && isVelocityRight))) {
			isBrakeAvailable = false;
			brakeTimer = MARIO_BRAKE_TIME;
		}
		// this catches brake applications from this update() call and previous update() calls
		if(brakeTimer > 0f) {
			doBrakeMove = true;
			brakeTimer -= delta;
		}

		// apply impulses if necessary
		if(doBrakeMove) {
			brakeLeftRight(isFacingRight);
			returnState = MarioCharState.BRAKE;
		}
		else if(doWalkRunMove) {
			moveBodyLeftRight(wantsToGoRight, wantsToRun);
			returnState = MarioCharState.WALKRUN;
		}
		else if(doDecelMove) {
			decelLeftRight(isFacingRight);
			returnState = MarioCharState.WALKRUN;
		}

		// Do not check mario's "on ground" status for a short time after mario jumps, because his foot sensor
		// might still be touching the ground even after his body enters the air.
		if(jumpGroundCheckTimer > delta)
			jumpGroundCheckTimer -= delta;
		else {
			jumpGroundCheckTimer = 0f;
			// The player can jump once per press of the jump key, so let them jump again when they release the
			// button but, they need to be on the ground with the button released.
			if(isOnGround) {
				isJumping = false;
				if(!wantsToJump)
					isNewJumpAllowed = true;
			}
		}

		// jump?
		if(wantsToJump && isNewJumpAllowed) {	// do jump
			isNewJumpAllowed = false;
			isJumping = true;
			// start a timer to delay checking for onGround status
			jumpGroundCheckTimer = MARIO_JUMP_GROUNDCHECK_DELAY;
			returnState = MarioCharState.JUMP;

			// the faster mario is moving, the higher he jumps, up to a max
			float mult = Math.abs(b2body.getLinearVelocity().x) / MARIO_MAX_RUNJUMPVEL;
			// cap the multiplier
			if(mult > 1f)
				mult = 1f;

			mult *= (float) MARIO_RUNJUMP_MULT;
			mult += 1f;

			// apply initial (and only) jump impulse
			moveBodyY(MARIO_JUMP_IMPULSE * mult);
			// the remainder of the jump up velocity is achieved through mid-air up-force
			jumpForceTimer = MARIO_JUMPFORCE_TIME;
			if(curPowerState != MarioPowerState.SMALL)
				runner.playSound(GameInfo.SOUND_MARIOBIGJUMP);
			else
				runner.playSound(GameInfo.SOUND_MARIOSMLJUMP);
		}
		else if(isJumping) {	// jumped and is mid-air
			returnState = MarioCharState.JUMP;
			// jump force stops, and cannot be restarted, if the player releases the jump key
			if(!wantsToJump)
				jumpForceTimer = 0f;
			// The longer the player holds the jump key, the higher they go,
			// if mario is moving up (no jump force allowed while mario is moving down)
			// TODO: what if mario is initally moving down because he jumped from an elevator?
			else if(b2body.getLinearVelocity().y > 0f && jumpForceTimer > 0f) {
				jumpForceTimer -= delta;
				// the force was strong to begin and tapered off over time - some said it became irrelevant
				useTheForceMario(MARIO_JUMP_FORCE * jumpForceTimer / MARIO_JUMPFORCE_TIME);
			}
		}
		// finally, if mario is not on the ground (for reals) then he is falling since he is not jumping
		else if(!isOnGround && jumpGroundCheckTimer <= 0f) {
			// cannot jump while falling
			isNewJumpAllowed = false;
			returnState = MarioCharState.FALL;
		}

		if(isDucking)
			return MarioCharState.DUCK;
		else
			return returnState;
	}

	@Override
	public void update(float delta) {
		MarioCharState nextState;
		boolean threwFireball;
		boolean isStarPowered;

		processHeadHits();	// hitting bricks with his head
		processHeadBounces();	// bouncing on heads of goombas, turtles, etc.
		threwFireball = processFireball(delta);
		processPowerups();
		processDamage(delta);

		nextState = processBodyState(delta);
		stateTimer = nextState == curCharState ? stateTimer + delta : 0f;
		curCharState = nextState;

		wantsToRunOnPrevUpdate = wantsToRun;

		wantsToGoRight = false;
		wantsToGoLeft = false;
		wantsToRun = false;
		wantsToJump = false;
		wantsToGoDown = false;

		isStarPowered = false;
		if(powerStarTimer > 0f) {
			isStarPowered = true;
			powerStarTimer -= delta;
			// restart regular music when powerstar powerup finishes
			if(powerStarTimer <= 0f)
				runner.startMusic(GameInfo.MUSIC_MARIO, true);
		}

		if(threwFireball)
			marioSprite.update(delta, b2body.getPosition(), MarioCharState.FIREBALL, curPowerState, isFacingRight, isDmgInvincible, isStarPowered, bodyFakeHeight);
		else
			marioSprite.update(delta, b2body.getPosition(), nextState, curPowerState, isFacingRight, isDmgInvincible, isStarPowered, bodyFakeHeight);
	}

	private void processHeadBounces() {
		if(isHeadBouncing) {
			isHeadBouncing = false;
			b2body.setLinearVelocity(b2body.getLinearVelocity().x, 0f);
			b2body.applyLinearImpulse(new Vector2(0f, MARIO_HEADBOUNCE_VEL), b2body.getWorldCenter(), true);
		}
	}

	// mario can shoot fireballs two at a time, but must wait if his "fireball timer" runs low
	private boolean processFireball(float delta) {
		fireballTimer += delta;
		if(fireballTimer > TIME_PER_FIREBALL)
			fireballTimer = TIME_PER_FIREBALL;

		// fire a ball?
		if(curPowerState == MarioPowerState.FIRE && wantsToRun && !wantsToRunOnPrevUpdate && fireballTimer > 0f) {
			fireballTimer -= TIME_PER_FIREBALL;
			throwFireball();
			return true;
		}

		return false;
	}

	private void throwFireball() {
		MarioFireball ball;

		if(isFacingRight)
			ball = new MarioFireball(runner, b2body.getPosition().cpy().add(FIREBALL_OFFSET, 0f), true);
		else
			ball = new MarioFireball(runner, b2body.getPosition().cpy().add(-FIREBALL_OFFSET, 0f), false);

		runner.addRobot(ball);
		runner.playSound(GameInfo.SOUND_FIREBALL);
	}

	private void decelLeftRight(boolean right) {
		float vx = b2body.getLinearVelocity().x;
		if(vx == 0f)
			return;

		if(vx > 0f)
			b2body.applyLinearImpulse(new Vector2(-DECEL_XIMP, 0f), b2body.getWorldCenter(), true);
		else if(vx < 0f)
			b2body.applyLinearImpulse(new Vector2(DECEL_XIMP, 0f), b2body.getWorldCenter(), true);

		// do not decel so hard he moves in opposite direction
		if((vx > 0f && b2body.getLinearVelocity().x < 0f) || (vx < 0f && b2body.getLinearVelocity().x > 0f))
			b2body.setLinearVelocity(0f, b2body.getLinearVelocity().y);
	}

	private void moveBodyLeftRight(boolean right, boolean doRunRun) {
		float speed, max;
		if(isOnGround)
			speed = doRunRun ? MARIO_RUNMOVE_XIMP : MARIO_WALKMOVE_XIMP;
		else {
			speed = MARIO_AIRMOVE_XIMP;
			if(isDucking)
				speed /= 2f;
		}
		if(doRunRun)
			max = MARIO_MAX_RUNVEL;
		else
			max = MARIO_MAX_WALKVEL;
		if(right && b2body.getLinearVelocity().x <= max)
			b2body.applyLinearImpulse(new Vector2(speed, 0f), b2body.getWorldCenter(), true);
		else if(!right && b2body.getLinearVelocity().x >= -max)
			b2body.applyLinearImpulse(new Vector2(-speed, 0f), b2body.getWorldCenter(), true);
	}

	private void brakeLeftRight(boolean right) {
		float vx = b2body.getLinearVelocity().x;
		if(vx == 0f)
			return;

		if(right && vx < 0f)
			b2body.applyLinearImpulse(new Vector2(MARIO_BRAKE_XIMP, 0f), b2body.getWorldCenter(),  true);
		else if(!right && vx > 0f)
			b2body.applyLinearImpulse(new Vector2(-MARIO_BRAKE_XIMP, 0f), b2body.getWorldCenter(),  true);

		// do not brake so hard he moves in opposite direction
		if((vx > 0f && b2body.getLinearVelocity().x < 0f) || (vx < 0f && b2body.getLinearVelocity().x > 0f))
			b2body.setLinearVelocity(0f, b2body.getLinearVelocity().y);
	}

	private void moveBodyY(float value) {
		b2body.applyLinearImpulse(new Vector2(0, value),
				b2body.getWorldCenter(), true);
	}

	private void useTheForceMario(float notMyFather) {
		b2body.applyForce(new Vector2(0, notMyFather), b2body.getWorldCenter(), true);
	}

	private void processPowerups() {
		// apply powerup if received
		switch(receivedPowerup) {
			case MUSHROOM:
				if(curPowerState == MarioPowerState.SMALL) {
					curPowerState = MarioPowerState.BIG;
					defineBody(b2body.getPosition().add(0f, GameInfo.P2M(8f)), b2body.getLinearVelocity());
					runner.playSound(GameInfo.SOUND_POWERUP_USE);
				}
				break;
			case FIREFLOWER:
				if(curPowerState == MarioPowerState.SMALL) {
					curPowerState = MarioPowerState.BIG;
					defineBody(b2body.getPosition().add(0f, GameInfo.P2M(8f)), b2body.getLinearVelocity());
					runner.playSound(GameInfo.SOUND_POWERUP_USE);
				}
				else if(curPowerState == MarioPowerState.BIG) {
					curPowerState = MarioPowerState.FIRE;
					defineBody(b2body.getPosition(), b2body.getLinearVelocity());
					runner.playSound(GameInfo.SOUND_POWERUP_USE);
				}
				break;
			case POWERSTAR:
				powerStarTimer = POWERSTAR_TIME;
				runner.playSound(GameInfo.SOUND_POWERUP_USE);
				runner.startMusic(GameInfo.MUSIC_STARPOWER, false);
				break;
			case NONE:
				break;
		}

		receivedPowerup = PowerupType.NONE;
	}

	private void processDamage(float delta) {
		if(dmgInvincibleTime > 0f)
			dmgInvincibleTime -= delta;
		else if(isDmgInvincible)
			endDmgInvincibility();

		// apply damage if received
		if(isTakeDamage) {
			isTakeDamage = false;
			// fire mario loses fire
			// big mario gets smaller
			if(curPowerState == MarioPowerState.FIRE || curPowerState == MarioPowerState.BIG) {
				curPowerState = MarioPowerState.SMALL;
				if(isDucking) {
					isDucking = false;
					defineBody(b2body.getPosition(), b2body.getLinearVelocity());
				}
				else
					defineBody(b2body.getPosition().sub(0f, GameInfo.P2M(8f)), b2body.getLinearVelocity());
				
				startDmgInvincibility();
				runner.playSound(GameInfo.SOUND_POWERDOWN);
			}
			// die if small and not invincible
			else
				die();
		}
	}

	private void startDmgInvincibility() {
		isDmgInvincible = true;
		dmgInvincibleTime = DMG_INVINCIBLE_TIME;

		// ensure mario cannot collide with enemies
		Filter filter = new Filter();
		filter.categoryBits = GameInfo.MARIO_BIT;
		filter.maskBits = GameInfo.BOUNDARY_BIT;
		marioBodyFixture.setFilterData(filter);
	}

	private void endDmgInvincibility() {
		isDmgInvincible = false;
		dmgInvincibleTime = 0f;

		Filter filter = new Filter();
		filter.categoryBits = GameInfo.MARIO_BIT;
		filter.maskBits = GameInfo.BOUNDARY_BIT | GameInfo.ROBOT_BIT;
		marioBodyFixture.setFilterData(filter);
	}

	// process the list of head hits for a head bang
	private void processHeadHits() {
		float closest;
		InteractiveTileObject closestTile;

		// check the list of tiles for the closest to mario
		closest = GameInfo.MAX_FLOAT_HACK;
		closestTile = null;
		for(InteractiveTileObject thingHit : headHits) {
			float dist;
			dist = Math.abs(thingHit.getPosition().x - b2body.getPosition().x);
			if(closestTile == null || dist < closest) {
				closest = dist;
				closestTile = thingHit;
			}
		}
		headHits.clear();

		// we have a weiner!
		if(closestTile != null) {
			canHeadBang = false;
			closestTile.onHeadHit(this);
		}
		else {
			// mario can headbang once per up/down cycle of movement, so re-enable head bang when mario moves down
			if(b2body.getLinearVelocity().y < 0f)
				canHeadBang = true;
		}
	}

	private void defineBody(Vector2 position, Vector2 velocity) {
		BodyDef bdef;
		FixtureDef fdef;
		PolygonShape marioShape;
		PolygonShape headSensor;
		PolygonShape footSensor;

		if(b2body != null)
			runner.getWorld().destroyBody(b2body);

		bdef = new BodyDef();
		bdef.position.set(position);
		bdef.linearVelocity.set(velocity);
		bdef.type = BodyDef.BodyType.DynamicBody;
		b2body = runner.getWorld().createBody(bdef);

		fdef = new FixtureDef();
		marioShape = new PolygonShape();
		if(curPowerState == MarioPowerState.SMALL || isDucking)
			marioShape.setAsBox(GameInfo.P2M(7f),  GameInfo.P2M(6f));
		else
			marioShape.setAsBox(GameInfo.P2M(7f),  GameInfo.P2M(13f));

		fdef.filter.categoryBits = GameInfo.MARIO_BIT;
		fdef.filter.maskBits = GameInfo.BOUNDARY_BIT;

		fdef.shape = marioShape;
		// mario should slide easily, but still have some friction to prevent sliding forever
		fdef.friction = 0.01f;	// (default is 0.2f)
		marioBodyFixture = b2body.createFixture(fdef);
		marioBodyFixture.setUserData(this);

		// head sensor for detecting head banging behavior
		headSensor = new PolygonShape();
		if(curPowerState == MarioPowerState.SMALL || isDucking)
			headSensor.setAsBox(GameInfo.P2M(5f), GameInfo.P2M(1f), new Vector2(GameInfo.P2M(0f), GameInfo.P2M(8f)), 0f);
		else
			headSensor.setAsBox(GameInfo.P2M(5f), GameInfo.P2M(1f), new Vector2(GameInfo.P2M(0f), GameInfo.P2M(16f)), 0f);
		fdef.filter.categoryBits = GameInfo.MARIOHEAD_BIT;
		fdef.filter.maskBits = GameInfo.BANGABLE_BIT;
		fdef.shape = headSensor;
		fdef.isSensor = true;
		b2body.createFixture(fdef).setUserData(this);

		// foot sensor for detecting onGround
		footSensor = new PolygonShape();
		if(curPowerState == MarioPowerState.SMALL || isDucking)
			footSensor.setAsBox(GameInfo.P2M(5f), GameInfo.P2M(2f), new Vector2(0f, GameInfo.P2M(-6)), 0f);
		else
			footSensor.setAsBox(GameInfo.P2M(5f), GameInfo.P2M(2f), new Vector2(0f, GameInfo.P2M(-16)), 0f);
		fdef.filter.categoryBits = GameInfo.MARIOFOOT_BIT;
		fdef.filter.maskBits = GameInfo.BOUNDARY_BIT;
		fdef.shape = footSensor;
		fdef.isSensor = true;
		b2body.createFixture(fdef).setUserData(this);

		// Create a robot sensor, so that mario doesn't collide with goombas or items like mushrooms and slow down -
		// he should only sense when they contact
		fdef.filter.categoryBits = GameInfo.MARIO_ROBOT_SENSOR_BIT;
		fdef.filter.maskBits = GameInfo.ROBOT_BIT | GameInfo.ITEM_BIT;
		fdef.shape = marioShape;
		fdef.isSensor = true;
		b2body.createFixture(fdef).setUserData(this);

		// not really "fake" height", rather it's the ideal height in modified pixel units 
		if(curPowerState == MarioPowerState.SMALL || isDucking)
			bodyFakeHeight = GameInfo.P2M(16f);
		else
			bodyFakeHeight = GameInfo.P2M(32f);
	}

	private void die() {
		if(!marioIsDead) {
			marioIsDead = true;
			runner.playSound(GameInfo.SOUND_MARIODIE);
			runner.stopMusic();
			
			Filter filter = new Filter();
			filter.maskBits = GameInfo.NOTHING_BIT;
			for(Fixture fixture : b2body.getFixtureList())
				fixture.setFilterData(filter);

			b2body.setLinearVelocity(0f, 0f);
			b2body.applyLinearImpulse(new Vector2(0, 4f), b2body.getWorldCenter(), true);
		}
	}

	public void applyPowerup(PowerupType powerup) {
		// TODO: check if already received powerup, and check for rank
		receivedPowerup = powerup;
	}

	@Override
	public boolean isDead() {
		return marioIsDead;
	}

	@Override
	public Body getB2Body() {
		return b2body;
	}

	@Override
	public void draw(Batch batch) {
		marioSprite.draw(batch);
	}

	@Override
	public void rightIt() {
		wantsToGoRight = true;
	}

	@Override
	public void leftIt() {
		wantsToGoLeft = true;
	}

	public void downIt() {
		wantsToGoDown = true;
	}

	@Override
	public void runIt() {
		wantsToRun = true;
	}

	@Override
	public void jumpIt() {
		wantsToJump = true;
	}

	// Foot sensor might come into contact with multiple boundary lines, so increment for each contact start,
	// and decrement for each contact end. If onGroundCount reaches zero then mario's foot sensor is not touching
	// a boundary line, hence mario is not on the ground.
	@Override
	public void onFootTouchBound(LineSeg seg) {
		if(!seg.isHorizontal)
			return;

		onGroundCount++;
		isOnGround = true;
	}

	@Override
	public void onFootLeaveBound(LineSeg seg) {
		if(!seg.isHorizontal)
			return;

		onGroundCount--;
		if(onGroundCount == 0)
			isOnGround = false;
	}

	private float getCurrentBodyHeight() {
		switch(curPowerState ) {
			case BIG:
			case FIRE:
				if(isDucking)
					return GameInfo.P2M(16);
				else
					return GameInfo.P2M(32);
			case SMALL:
			default:
				return GameInfo.P2M(16);
		}
	}

	@Override
	public void onTouchRobot(RobotRole robo) {
		// If the bottom of mario sprite is at least as high as the middle point of the robot sprite, then the robot
		// takes damage. Otherwise mario takes damage.
		float marioY = b2body.getPosition().y;
		float robotY = robo.getBody().getPosition().y;
		float marioHeight = getCurrentBodyHeight();

		// touch end of level flagpole?
		if(robo instanceof Flagpole) {
			isLevelEnd = true;
			touchedFlagpole = (Flagpole) robo;
		}
		// test for powerstar damage
		else if(robo instanceof DamageableBot && powerStarTimer > 0f) {
			// playSound should go in the processBody method, but... this is so much easier!
			runner.playSound(GameInfo.SOUND_KICK);
			((DamageableBot) robo).onDamage(1f, b2body.getPosition());
		}
		// test for bounce on head
		else if(robo instanceof HeadBounceBot && marioY - (marioHeight/2f) >= robotY) {
			((HeadBounceBot) robo).onHeadBounce(b2body.getPosition());
			isHeadBouncing = true;
		}
		// does the robot do touch damage? (from non-head bounce source)
		else if(robo instanceof TouchDmgBot && ((TouchDmgBot) robo).isTouchDamage()) {
			if(dmgInvincibleTime > 0)
				return;
			isTakeDamage = true;
		}
		else if(robo instanceof Turtle)
			((Turtle) robo).onPlayerTouch(b2body.getPosition());	// push shell
	}

	@Override
	public void onTouchItem(RobotRole robo) {
		if(robo instanceof ItemRobot) {
			((ItemRobot) robo).use(this);
		}
	}

	@Override
	public void onHeadHit(InteractiveTileObject thing) {
		// After banging his head while moving up, mario cannot bang his head again until he has moved down a
		// sufficient amount.
		// Also, mario can only break one block per head bang - but if his head touches multiple blocks when
		// he hits, then choose the block closest to mario on the x axis.

		// if can bang and is moving up, keep track of things that head hit - check the list once per update
		if(canHeadBang && b2body.getLinearVelocity().y > 0f) {
			headHits.add(thing);
		}
	}

	@Override
	public float getStateTimer() {
		return stateTimer;
	}

	public boolean isBig() {
		return (curPowerState != MarioPowerState.SMALL);
	}
}
