package me._2818.partyTS.messages;

import net.kyori.adventure.text.Component;

public enum Message {
    // General messages
    PLAYER_ONLY("error.player-only"),
    NO_PERMISSION("error.no-permission"),
    ERROR_USAGE("command.usage-format"),
    
    // Party creation
    PARTY_CREATED("party.created"),
    PARTY_ALREADY_IN_ONE("party.already-in-one"),
    PARTY_CREATE_FAILED("party.create-failed"),
    PARTY_AUTO_CREATED("party.auto-created"),
    
    // Invites
    INVITE_SENT("invite.sent"),
    INVITE_RECEIVED("invite.received"),
    INVITE_ACCEPTED("invite.accepted"),
    INVITE_ACCEPTED_NOTIFICATION("invite.accepted-notification"),
    INVITE_DECLINED("invite.declined"),
    INVITE_DECLINED_NOTIFICATION("invite.declined-notification"),
    INVITE_EXPIRED("invite.expired"),
    INVITE_EXPIRED_NOTIFICATION("invite.expired-notification"),
    INVITE_ALREADY_INVITED("invite.already-invited"),
    INVITE_ALREADY_IN_PARTY("invite.already-in-party"),
    INVITE_COOLDOWN("invite.cooldown"),
    
    // Member actions
    MEMBER_JOINED("member.joined"),
    MEMBER_LEFT("member.left"),
    MEMBER_KICKED("member.kicked"),
    MEMBER_KICKED_OTHER("member.kicked-other"),
    
    // Errors
    ERROR_PLAYER_NOT_FOUND("error.player-not-found"),
    ERROR_NO_INVITE("error.no-invite"),
    ERROR_NOT_IN_PARTY("error.not-in-party"),
    ERROR_NOT_LEADER("error.not-leader"),
    ERROR_CANNOT_KICK_SELF("error.cannot-kick-self"),
    
    // Race
    RACE_STARTING("race.starting"),
    RACE_FINISHED("race.finished"),
    RACE_PLAYER_FINISHED("race.player-finished"),
    RACE_TRACK_NOT_FOUND("race.track-not-found"),
    RACE_INVALID_LAPS("race.invalid-laps"),
    RACE_INVALID_PITS("race.invalid-pits"),
    RACE_NOT_LEADER("race.not-leader");
    
    private final String key;
    
    Message(String key) {
        this.key = key;
    }
    
    public String getKey() {
        return key;
    }
    
    public Component asComponent(Object... args) {
        return LanguageManager.getInstance().getComponent(this, args);
    }
    
    public String asString(Object... args) {
        return LanguageManager.getInstance().getString(this, args);
    }
}
