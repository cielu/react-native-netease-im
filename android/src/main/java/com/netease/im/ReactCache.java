package com.netease.im;

import android.text.TextUtils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.netease.im.login.LoginService;
import com.netease.im.session.extension.BankTransferAttachment;
import com.netease.im.session.extension.CustomAttachment;
import com.netease.im.session.extension.CustomAttachmentType;
import com.netease.im.session.extension.ExtendsionAttachment;
import com.netease.im.session.extension.RedPackageAttachement;
import com.netease.im.uikit.cache.FriendDataCache;
import com.netease.im.uikit.cache.NimUserInfoCache;
import com.netease.im.uikit.cache.TeamDataCache;
import com.netease.im.uikit.common.util.log.LogUtil;
import com.netease.im.uikit.common.util.sys.TimeUtil;
import com.netease.im.uikit.contact.core.item.AbsContactItem;
import com.netease.im.uikit.contact.core.item.ContactItem;
import com.netease.im.uikit.contact.core.model.ContactDataList;
import com.netease.im.uikit.contact.core.model.IContact;
import com.netease.im.uikit.contact.core.model.TeamContact;
import com.netease.im.uikit.session.helper.TeamNotificationHelper;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.friend.model.AddFriendNotify;
import com.netease.nimlib.sdk.media.record.RecordType;
import com.netease.nimlib.sdk.msg.attachment.AudioAttachment;
import com.netease.nimlib.sdk.msg.attachment.ImageAttachment;
import com.netease.nimlib.sdk.msg.attachment.LocationAttachment;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;
import com.netease.nimlib.sdk.msg.attachment.VideoAttachment;
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SystemMessageStatus;
import com.netease.nimlib.sdk.msg.constant.SystemMessageType;
import com.netease.nimlib.sdk.msg.model.AttachmentProgress;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.msg.model.SystemMessage;
import com.netease.nimlib.sdk.team.model.Team;
import com.netease.nimlib.sdk.team.model.TeamMember;
import com.netease.nimlib.sdk.uinfo.UserInfoProvider;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dowin on 2017/4/28.
 */

public class ReactCache {
    public final static String observeRecentContact = "observeRecentContact";//'最近会话'
    public final static String observeOnlineStatus = "observeOnlineStatus";//'在线状态'
    public final static String observeFriend = "observeFriend";//'联系人'
    public final static String observeTeam = "observeTeam";//'群组'
    public final static String observeReceiveMessage = "observeReceiveMessage";//'接收消息'
    public final static String observeReceiveSystemMsg = "observeReceiveSystemMsg";//'系统通知'
    public final static String observeMsgStatus = "observeMsgStatus";//'发送消息状态变化'
    public final static String observeAudioRecord = "observeAudioRecord";//'录音状态'
    public final static String observeUnreadCountChange = "observeUnreadCountChange";//'未读数变化'
    public final static String observeBlackList = "observeBlackList";//'黑名单'
    public final static String observeAttachmentProgress = "observeAttachmentProgress";//'上传下载进度'

    final static String TAG = "ReactCache";
    private static ReactContext reactContext;

    public static void setReactContext(ReactContext reactContext) {
        ReactCache.reactContext = reactContext;
    }

    public static ReactContext getReactContext() {
        return reactContext;
    }

    public static void emit(String eventName, Object date) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, date);
    }

    public static Object createRecentList(List<RecentContact> recents, int unreadNum) {
        LogUtil.i(TAG, "size:" + (recents == null ? 0 : recents.size()));
        // recents参数即为最近联系人列表（最近会话列表）
        WritableMap writableMap = Arguments.createMap();
        WritableArray array = Arguments.createArray();
        int unreadNumTotal=0;
        if (recents != null && recents.size() > 0) {

            WritableMap map;
            for (RecentContact contact : recents) {
                map = Arguments.createMap();
                String contactId = contact.getContactId();
                unreadNumTotal += contact.getUnreadCount();
                map.putString("contactId", contactId);
                map.putString("unreadCount", String.valueOf(contact.getUnreadCount()));
                String name = "";
                if (contact.getSessionType() == SessionTypeEnum.P2P) {
                    map.putString("teamType", "-1");
                    NimUserInfoCache nimUserInfoCache = NimUserInfoCache.getInstance();
                    map.putString("imagePath", nimUserInfoCache.getAvatar(contactId));
                    name = nimUserInfoCache.getUserDisplayName(contactId);
                } else if (contact.getSessionType() == SessionTypeEnum.Team) {
                    Team team = TeamDataCache.getInstance().getTeamById(contactId);
                    if (team != null) {
                        name = team.getName();
                        map.putString("teamType", Integer.toString(team.getType().getValue()));
                        map.putString("imagePath", team.getIcon());
                    }
                }
                map.putString("name", name);
                map.putString("sessionType", Integer.toString(contact.getSessionType().getValue()));
                map.putString("msgType", Integer.toString(contact.getMsgType().getValue()));
                map.putString("msgStatus", Integer.toString(contact.getMsgStatus().getValue()));
                map.putString("messageId", contact.getRecentMessageId());
//
                map.putString("fromAccount", contact.getFromAccount());

                String content = contact.getContent();
                map.putString("time", TimeUtil.getTimeShowString(contact.getTime(), true));

                String fromNick = "";
                try {
                    fromNick = contact.getFromNick();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                fromNick =  TextUtils.isEmpty(fromNick) ? NimUserInfoCache.getInstance().getUserDisplayName(contact.getFromAccount()) : fromNick;
                map.putString("nick", fromNick);
                String nickName = "";
                if (contact.getSessionType() == SessionTypeEnum.Team) {
                    nickName = fromNick + "：";
                }
                MsgAttachment attachment = contact.getAttachment();
                if (attachment != null) {
                    if (attachment instanceof RedPackageAttachement) {
                        map.putInt("custType", CustomAttachmentType.RedPackage);
                        content = "[红 包]";
                    } else if (attachment instanceof BankTransferAttachment) {
                        map.putInt("custType", CustomAttachmentType.RTS);
                        content = "[转 账]";
                    } else if (attachment instanceof ExtendsionAttachment) {
                        ExtendsionAttachment extendsionAttachment = (ExtendsionAttachment) attachment;
                        content = extendsionAttachment.getRecentValue();
                    }
                }
                map.putString("content", nickName + content);
                array.pushMap(map);
            }
            LogUtil.i(TAG, array + "");
        }
        writableMap.putArray("recents", array);
        writableMap.putString("unreadCount", Integer.toString(unreadNumTotal));
        return writableMap;
    }

    static Pattern pattern = Pattern.compile("\\d{5}");

    static boolean hasFilterFriend(String contactId) {
        if (contactId != null) {
            if (contactId.equals(LoginService.getInstance().getAccount())) {
                return true;
            }
            if (contactId.length() == 5) {
                Matcher matcher = pattern.matcher(contactId);
                if (matcher.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 过滤自己 和 \d{5}账号
     *
     * @param dataList
     * @return
     */
    public static Object createFriendList(ContactDataList dataList, boolean hasFilter) {
        LogUtil.i(TAG, dataList.getCount() + "");
        WritableArray array = Arguments.createArray();
        if (dataList != null) {
            int count = dataList.getCount();
            for (int i = 0; i < count; i++) {
                AbsContactItem item = dataList.getItem(i);

                if (item instanceof ContactItem) {

                    ContactItem contactItem = (ContactItem) item;
                    IContact contact = contactItem.getContact();
                    String contactId = contact.getContactId();
                    if (hasFilter && hasFilterFriend(contactId)) {
                        continue;
                    }
                    String belongs = contactItem.belongsGroup();
                    WritableMap map = Arguments.createMap();

                    map.putString("itemType", Integer.toString(contactItem.getItemType()));
                    map.putString("belong", belongs);
                    map.putString("contactId", contactId);

                    map.putString("alias", contact.getDisplayName());
                    map.putString("type", Integer.toString(contact.getContactType()));
                    map.putString("name", NimUserInfoCache.getInstance().getUserName(contactId));
                    map.putString("avatar", NimUserInfoCache.getInstance().getAvatar(contactId));
                    array.pushMap(map);
//                } else {
//                map.putString("itemType", Integer.toString(item.getItemType()));
//                map.putString("belong", item.belongsGroup());
                }
            }
        }
        LogUtil.i(TAG, array + "");
        return array;
    }


    public static Object createFriendSet(ContactDataList datas, boolean hasFilter) {

        WritableMap writableMap = Arguments.createMap();
        if (datas != null) {
            Map<String, WritableArray> listHashMap = new HashMap<>();
            int count = datas.getCount();
            for (int i = 0; i < count; i++) {
                AbsContactItem item = datas.getItem(i);

                if (item instanceof ContactItem) {
                    ContactItem contactItem = (ContactItem) item;
                    IContact contact = contactItem.getContact();
                    String contactId = contact.getContactId();
                    if (hasFilter && hasFilterFriend(contactId)) {
                        continue;
                    }
                    String belongs = contactItem.belongsGroup();
                    WritableMap map = Arguments.createMap();

                    map.putString("itemType", Integer.toString(contactItem.getItemType()));
                    map.putString("belong", belongs);
                    map.putString("contactId", contact.getContactId());

                    map.putString("alias", contact.getDisplayName());
                    map.putString("type", Integer.toString(contact.getContactType()));
                    map.putString("name", NimUserInfoCache.getInstance().getUserName(contact.getContactId()));
                    map.putString("avatar", NimUserInfoCache.getInstance().getAvatar(contact.getContactId()));
                    WritableArray array = listHashMap.get(belongs);
                    if (array == null) {
                        array = Arguments.createArray();
                    }
                    array.pushMap(map);

                    listHashMap.put(belongs, array);
//                } else {
//                map.putString("itemType", Integer.toString(item.getItemType()));
//                map.putString("belong", item.belongsGroup());
                }
            }
            if (listHashMap.size() > 0) {
                for (Map.Entry<String, WritableArray> entry : listHashMap.entrySet()) {
                    writableMap.putArray(entry.getKey(), entry.getValue());
                }
            }
            listHashMap.clear();
        }

        LogUtil.i(TAG, writableMap + "");
        return writableMap;
    }

    public static Object createTeamList(ContactDataList datas) {

        WritableArray writableArray = Arguments.createArray();
        if (datas != null) {
            int count = datas.getCount();
            for (int i = 0; i < count; i++) {
                AbsContactItem item = datas.getItem(i);

                if (item instanceof ContactItem) {
                    ContactItem contactItem = (ContactItem) item;
                    if (contactItem.getContact() instanceof TeamContact) {
                        String belongs = contactItem.belongsGroup();
                        WritableMap map = Arguments.createMap();

                        map.putString("itemType", Integer.toString(contactItem.getItemType()));
                        map.putString("belong", belongs);

                        TeamContact teamContact = (TeamContact) contactItem.getContact();
                        map.putString("teamId", teamContact.getContactId());
                        if (teamContact.getTeam() != null) {
                            map.putString("teamType", Integer.toString(teamContact.getTeam().getType().getValue()));
                        }
                        map.putString("name", teamContact.getDisplayName());
                        map.putString("type", Integer.toString(teamContact.getContactType()));
                        map.putString("avatar", NimUserInfoCache.getInstance().getAvatar(teamContact.getContactId()));
                        writableArray.pushMap(map);
                    }
//                } else {
//                map.putString("itemType", Integer.toString(item.getItemType()));
//                map.putString("belong", item.belongsGroup());
                }
            }
        }

        LogUtil.i(TAG, writableArray + "");
        return writableArray;
    }

    /**
     * account 账号
     * name 用户名
     * avatar 头像
     * signature 签名
     * gender 性别
     * email
     * birthday
     * mobile
     * extension扩展
     * extensionMap扩展map
     */
    public static Object createUserInfo(NimUserInfo userInfo) {
        WritableMap writableMap = Arguments.createMap();
        if (userInfo != null) {

            writableMap.putString("isMyFriend", boolean2String(FriendDataCache.getInstance().isMyFriend(userInfo.getAccount())));
            writableMap.putString("isMe", boolean2String(userInfo.getAccount() != null && userInfo.getAccount().equals(LoginService.getInstance().getAccount())));
            writableMap.putString("isInBlackList", boolean2String(NIMClient.getService(FriendService.class).isInBlackList(userInfo.getAccount())));
            writableMap.putString("mute", boolean2String(NIMClient.getService(FriendService.class).isNeedMessageNotify(userInfo.getAccount())));

            writableMap.putString("contactId", userInfo.getAccount());
            writableMap.putString("name", userInfo.getName());
            writableMap.putString("alias", NimUserInfoCache.getInstance().getUserDisplayName(userInfo.getAccount()));
            writableMap.putString("avatar", userInfo.getAvatar());
            writableMap.putString("signature", userInfo.getSignature());
            writableMap.putString("gender", Integer.toString(userInfo.getGenderEnum().getValue()));
            writableMap.putString("email", userInfo.getEmail());
            writableMap.putString("birthday", userInfo.getBirthday());
            writableMap.putString("mobile", userInfo.getMobile());
        }
        return writableMap;
    }

    public static Object createSystemMsg(List<SystemMessage> sysItems) {
        WritableArray writableArray = Arguments.createArray();

        if (sysItems != null && sysItems.size() > 0) {
            NimUserInfoCache nimUserInfoCache = NimUserInfoCache.getInstance();
            for (SystemMessage message : sysItems) {
                WritableMap map = Arguments.createMap();
                boolean verify = isVerifyMessageNeedDeal(message);
                map.putString("messageId", Long.toString(message.getMessageId()));
                map.putString("type", Integer.toString(message.getType().getValue()));
                map.putString("targetId", message.getTargetId());
                map.putString("fromAccount", message.getFromAccount());
                map.putString("avatar", nimUserInfoCache.getAvatar(message.getFromAccount()));
                map.putString("name", nimUserInfoCache.getUserDisplayNameEx(message.getFromAccount()));//alias
                map.putString("time", Long.toString(message.getTime() / 1000));
                map.putString("isVerify", String.valueOf(verify));
                map.putString("status", Integer.toString(message.getStatus().getValue()));
                map.putString("verifyText", getVerifyNotificationText(message));
                map.putString("verifyResult", "");
                if (verify) {
                    if (message.getStatus() != SystemMessageStatus.init) {
                        map.putString("verifyResult", getVerifyNotificationDealResult(message));
                    }
                }
                writableArray.pushMap(map);
            }
        }
        return writableArray;
    }

    private static String getVerifyNotificationText(SystemMessage message) {
        StringBuilder sb = new StringBuilder();
        String fromAccount = NimUserInfoCache.getInstance().getUserDisplayNameYou(message.getFromAccount());
        Team team = TeamDataCache.getInstance().getTeamById(message.getTargetId());
        if (team == null && message.getAttachObject() instanceof Team) {
            team = (Team) message.getAttachObject();
        }
        String teamName = team == null ? message.getTargetId() : team.getName();

        if (message.getType() == SystemMessageType.TeamInvite) {
            sb.append("邀请").append("你").append("加入群 ").append(teamName);
        } else if (message.getType() == SystemMessageType.DeclineTeamInvite) {
            sb.append(fromAccount).append("拒绝了群 ").append(teamName).append(" 邀请");
        } else if (message.getType() == SystemMessageType.ApplyJoinTeam) {
            sb.append("申请加入群 ").append(teamName);
        } else if (message.getType() == SystemMessageType.RejectTeamApply) {
            sb.append(fromAccount).append("拒绝了你加入群 ").append(teamName).append("的申请");
        } else if (message.getType() == SystemMessageType.AddFriend) {
            AddFriendNotify attachData = (AddFriendNotify) message.getAttachObject();
            if (attachData != null) {
                if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_DIRECT) {
                    sb.append("已添加你为好友");
                } else if (attachData.getEvent() == AddFriendNotify.Event.RECV_AGREE_ADD_FRIEND) {
                    sb.append("通过了你的好友请求");
                } else if (attachData.getEvent() == AddFriendNotify.Event.RECV_REJECT_ADD_FRIEND) {
                    sb.append("拒绝了你的好友请求");
                } else if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_VERIFY_REQUEST) {
                    sb.append(TextUtils.isEmpty(message.getContent()) ? "请求添加好友" : message.getContent());
                }
            }
        }

        return sb.toString();
    }

    /**
     * 是否验证消息需要处理（需要有同意拒绝的操作栏）
     */
    private static boolean isVerifyMessageNeedDeal(SystemMessage message) {
        if (message.getType() == SystemMessageType.AddFriend) {
            if (message.getAttachObject() != null) {
                AddFriendNotify attachData = (AddFriendNotify) message.getAttachObject();
                if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_DIRECT ||
                        attachData.getEvent() == AddFriendNotify.Event.RECV_AGREE_ADD_FRIEND ||
                        attachData.getEvent() == AddFriendNotify.Event.RECV_REJECT_ADD_FRIEND) {
                    return false; // 对方直接加你为好友，对方通过你的好友请求，对方拒绝你的好友请求
                } else if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_VERIFY_REQUEST) {
                    return true; // 好友验证请求
                }
            }
            return false;
        } else if (message.getType() == SystemMessageType.TeamInvite || message.getType() == SystemMessageType.ApplyJoinTeam) {
            return true;
        } else {
            return false;
        }
    }

    private static String getVerifyNotificationDealResult(SystemMessage message) {
        if (message.getStatus() == SystemMessageStatus.passed) {
            return "已同意";
        } else if (message.getStatus() == SystemMessageStatus.declined) {
            return "已拒绝";
        } else if (message.getStatus() == SystemMessageStatus.ignored) {
            return "已忽略";
        } else if (message.getStatus() == SystemMessageStatus.expired) {
            return "已过期";
        } else {
            return "未处理";
        }
    }

    public static Object createBlackList(List<UserInfoProvider.UserInfo> data) {
        WritableArray array = Arguments.createArray();
        if (data != null) {
            for (UserInfoProvider.UserInfo userInfo : data) {
                if (userInfo != null) {
                    WritableMap writableMap = Arguments.createMap();
                    writableMap.putString("contactId", userInfo.getAccount());
                    writableMap.putString("name", userInfo.getName());
                    writableMap.putString("avatar", userInfo.getAvatar());
                    array.pushMap(writableMap);
                }
            }
        }
        return array;
    }

    private static boolean needShowTime(Set<String> timedItems, IMMessage message) {
        return timedItems != null && timedItems.contains(message.getUuid());
    }

    /**
     * @param messageList
     * @return Object
     */
    public static Object createMessageList(List<IMMessage> messageList) {
        WritableArray writableArray = Arguments.createArray();

        if (messageList != null) {
            int size = messageList.size();
            for (int i = 0; i < size; i++) {
                IMMessage item = messageList.get(i);
                if (item == null) {
                    continue;
                }
                WritableMap itemMap = createMessage(item);
//                String timeStr = TimeUtil.getTimeShowString(item.getTime(), false);
//                if (timedItems != null && timedItems.contains(item.getUuid())) {//添加短时间处理，作为一条消息
//                    itemMap = Arguments.createMap();
//                    itemMap.putString("msgType", Integer.toString(MsgTypeEnum.tip.getValue() + 1));
//                    itemMap.putString("time", timeStr);
//                    writableArray.pushMap(itemMap);
//                }


                writableArray.pushMap(itemMap);
            }
        }
        return writableArray;
    }

    public static Object createTeamInfo(Team team) {
        WritableMap writableMap = Arguments.createMap();
        if (team != null) {
            writableMap.putString("teamId", team.getId());
            writableMap.putString("name", team.getName());
            writableMap.putString("avatar", team.getIcon());
            writableMap.putString("type", Integer.toString(team.getType().getValue()));
            writableMap.putString("introduce", team.getIntroduce());
            writableMap.putString("createTime", TimeUtil.getTimeShowString(team.getCreateTime(), true));
            writableMap.putString("creator", team.getCreator());
            writableMap.putString("mute", boolean2String(!team.mute()));
            writableMap.putString("memberCount", Integer.toString(team.getMemberCount()));
            writableMap.putString("memberLimit", Integer.toString(team.getMemberLimit()));
        }
        return writableMap;
    }

    // userId
// contactId 群成员ID
// type 类型：0普通成员 1拥有者 2管理员 3申请者
// teamNick  群名片
// isMute 是否禁言
// joinTime 加入时间
// isInTeam 是否在群
// isMe
    public static WritableMap createTeamMemberInfo(TeamMember teamMember) {
        WritableMap writableMap = Arguments.createMap();
        if (teamMember != null) {
            writableMap.putString("contactId", teamMember.getAccount());
            writableMap.putString("type", Integer.toString(teamMember.getType().getValue()));
            writableMap.putString("alias", NimUserInfoCache.getInstance().getUserDisplayName(teamMember.getAccount()));
            writableMap.putString("name", TeamDataCache.getInstance().getTeamMemberDisplayName(teamMember.getTid(), teamMember.getAccount()));
            writableMap.putString("joinTime", TimeUtil.getTimeShowString(teamMember.getJoinTime(), true));
            writableMap.putString("avatar", NimUserInfoCache.getInstance().getAvatar(teamMember.getAccount()));

            writableMap.putString("isInTeam", boolean2String(teamMember.isInTeam()));
            writableMap.putString("isMute", boolean2String(teamMember.isMute()));
            writableMap.putString("teamId", teamMember.getTid());
            writableMap.putString("isMe", boolean2String(TextUtils.equals(teamMember.getAccount(), LoginService.getInstance().getAccount())));
        }
        return writableMap;
    }

    public static Object createTeamMemberList(List<TeamMember> teamMemberList) {

        WritableArray array = Arguments.createArray();
        int size = teamMemberList.size();
        if (teamMemberList != null && size > 0) {
            for (int i = 0; i < size; i++) {
                TeamMember teamMember = teamMemberList.get(i);

                WritableMap writableMap = createTeamMemberInfo(teamMember);

                array.pushMap(writableMap);
            }
        }
        return array;
    }

    private static boolean receiveReceiptCheck(final IMMessage msg) {
        if (msg != null) {
            if (msg.getSessionType() == SessionTypeEnum.P2P
                    && msg.getDirect() == MsgDirectionEnum.Out
                    && msg.getMsgType() != MsgTypeEnum.tip
                    && msg.getMsgType() != MsgTypeEnum.notification
                    && msg.isRemoteRead()) {
                return true;
            } else {
                return msg.isRemoteRead();
            }
        }
        return false;
    }

    static String boolean2String(boolean bool) {
        return bool ? Integer.toString(1) : Integer.toString(0);
    }

    /**
     * <br/>uuid 消息ID
     * <br/>sessionId 会话id
     * <br/>sessionType  会话类型
     * <br/>fromNick 发送人昵称
     * <br/>msgType  消息类型
     * <br/>status 消息状态
     * <br/>direct 发送或接收
     * <br/>content 发送内容
     * <br/>time 发送时间
     * <br/>fromAccount 发送人账号
     *
     * @param item
     * @return
     */
    public static WritableMap createMessage(IMMessage item) {
        WritableMap itemMap = Arguments.createMap();
        itemMap.putString("_id", item.getUuid());

        itemMap.putString("msgType", Integer.toString(item.getMsgType().getValue()));
        itemMap.putString("createdAt", Long.toString(item.getTime() / 1000));
        itemMap.putString("sessionId", item.getSessionId());
        itemMap.putString("sessionType", Integer.toString(item.getSessionType().getValue()));

        itemMap.putString("direct", Integer.toString(item.getDirect().getValue()));
        itemMap.putString("status", Integer.toString(item.getStatus().getValue()));
        itemMap.putString("attachStatus", Integer.toString(item.getAttachStatus().getValue()));
        itemMap.putString("isRemoteRead", boolean2String(receiveReceiptCheck(item)));

        WritableMap user = Arguments.createMap();
        user.putString("_id", item.getFromAccount());
        user.putString("name", NimUserInfoCache.getInstance().getUserDisplayName(item.getFromAccount()));
        user.putString("avatar", NimUserInfoCache.getInstance().getAvatar(item.getFromAccount()));
        itemMap.putMap("user", user);

        MsgAttachment attachment = item.getAttachment();
        String text = "";
        if (attachment != null) {
            if (item.getMsgType() == MsgTypeEnum.image) {
                WritableMap imageObj = Arguments.createMap();
                if (attachment instanceof ImageAttachment) {
                    ImageAttachment imageAttachment = (ImageAttachment) attachment;
                    imageObj.putString("thumbPath", imageAttachment.getThumbPathForSave());
                    imageObj.putString("thumbPath2", imageAttachment.getThumbPath());
                    imageObj.putString("path", imageAttachment.getPathForSave());
                    imageObj.putString("path2", imageAttachment.getPath());
                    imageObj.putString("url", imageAttachment.getUrl());
                    imageObj.putString("displayName", imageAttachment.getDisplayName());
                    imageObj.putString("imageHeight", Integer.toString(imageAttachment.getHeight()));
                    imageObj.putString("imageWidth", Integer.toString(imageAttachment.getWidth()));
                }
                itemMap.putMap("imageObj", imageObj);
            } else if (item.getMsgType() == MsgTypeEnum.audio) {

                WritableMap audioObj = Arguments.createMap();
                if (attachment instanceof AudioAttachment) {
                    AudioAttachment audioAttachment = (AudioAttachment) attachment;
                    audioObj.putString("path", audioAttachment.getPath());
                    audioObj.putString("path2", audioAttachment.getThumbPathForSave());
                    audioObj.putString("url", audioAttachment.getUrl());
                    audioObj.putString("duration", Long.toString(audioAttachment.getDuration()));
                }
                itemMap.putMap("audioObj", audioObj);
            } else if (item.getMsgType() == MsgTypeEnum.video) {
                WritableMap videoDic = Arguments.createMap();
                if (attachment instanceof VideoAttachment) {
                    VideoAttachment videoAttachment = (VideoAttachment) attachment;
                    videoDic.putString("url", videoAttachment.getUrl());
                    videoDic.putString("path", videoAttachment.getPathForSave());
                    videoDic.putString("path2", videoAttachment.getPath());
                    videoDic.putString("displayName", videoAttachment.getDisplayName());
                    videoDic.putString("coverSizeHeight", Integer.toString(videoAttachment.getHeight()));
                    videoDic.putString("coverSizeWidth", Integer.toString(videoAttachment.getWidth()));
                    videoDic.putString("duration", Long.toString(videoAttachment.getDuration()));
                    videoDic.putString("fileLength", Long.toString(videoAttachment.getSize()));

                    videoDic.putString("coverUrl", videoAttachment.getThumbPath());
                    videoDic.putString("coverPath", videoAttachment.getThumbPathForSave());
                }
                itemMap.putMap("videoDic", videoDic);
            } else if (item.getMsgType() == MsgTypeEnum.location) {
                WritableMap locationObj = Arguments.createMap();
                if (attachment instanceof LocationAttachment) {
                    LocationAttachment locationAttachment = (LocationAttachment) attachment;
                    locationObj.putString("latitude", Double.toString(locationAttachment.getLatitude()));
                    locationObj.putString("longitude", Double.toString(locationAttachment.getLongitude()));
                    locationObj.putString("title", locationAttachment.getAddress());
                }
                itemMap.putMap("locationObj", locationObj);
            } else if (item.getMsgType() == MsgTypeEnum.notification) {
                if (item.getSessionType() == SessionTypeEnum.Team) {
                    text = TeamNotificationHelper.getTeamNotificationText(item, item.getSessionId());
                } else {
                    text = item.getContent();
                }
                WritableMap notiObj = Arguments.createMap();
                notiObj.putString("tipMsg", text);
                itemMap.putMap("notiObj", notiObj);
            } else if (item.getMsgType() == MsgTypeEnum.custom) {//自定义消息
                try {
                    CustomAttachment customAttachment = (CustomAttachment) attachment;
                    itemMap.putString("custType", Integer.toString(customAttachment.getType()));

                    if (attachment instanceof RedPackageAttachement) {
                        RedPackageAttachement redPackageAttachement = (RedPackageAttachement) attachment;
                        itemMap.putMap("attachment", ReactExtendsion.createRedPackage(redPackageAttachement));

                    } else if (attachment instanceof BankTransferAttachment) {
                        BankTransferAttachment bankTransferAttachment = (BankTransferAttachment) attachment;
                        itemMap.putMap("attachment", ReactExtendsion.createBankTransfer(bankTransferAttachment));
                    }

//                    else if (attachment instanceof ExtendsionAttachment) {
//                        ExtendsionAttachment extendsionAttachment = (ExtendsionAttachment) attachment;
//                        itemMap.putMap("attachment", ReactExtendsion.makeHashMap2WritableMap(extendsionAttachment.getExtendsion()));
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } else {
            text = item.getContent();
        }
        if (item.getMsgType() == MsgTypeEnum.text) {
            text = item.getContent();

        } else if (item.getMsgType() == MsgTypeEnum.tip) {

            if (TextUtils.isEmpty(item.getContent())) {
                Map<String, Object> content = item.getRemoteExtension();
                if (content != null && !content.isEmpty()) {
                    text = (String) content.get("content");
                }
                content = item.getLocalExtension();
                if (content != null && !content.isEmpty()) {
                    text = (String) content.get("content");
                }
                if (TextUtils.isEmpty(text)) {
                    text = "未知通知提醒";
                }
            } else {
                text = item.getContent();
            }
            WritableMap notiObj = Arguments.createMap();
            notiObj.putString("tipMsg", text);
            itemMap.putMap("notiObj", notiObj);

        }
        itemMap.putString("content", text);
        return itemMap;
    }

    public static Object createAudioPlay(String type, long position) {
        WritableMap result = Arguments.createMap();
        result.putString("type", "play");
        result.putString("status", type);
        result.putString("playEnd", Long.toString(position));
        return result;
    }

    public static Object createAudioRecord(String type, File audioFile, long duration, RecordType recordType) {
        WritableMap result = Arguments.createMap();
        result.putString("type", "record");
        result.putString("status", type);
        result.putString("audioFile", audioFile == null ? "" : audioFile.getAbsolutePath());
        result.putString("currentTime", Long.toString(duration));
        result.putString("recordType", recordType == null ? "" : recordType.getFileSuffix());
        return result;
    }

    public static Object createAttachmentProgress(AttachmentProgress attachmentProgress) {
        WritableMap result = Arguments.createMap();
        result.putString("_id", attachmentProgress.getUuid());
        result.putString("total", Long.toString(attachmentProgress.getTotal()));
        result.putString("transferred", Long.toString(attachmentProgress.getTransferred()));

        return result;
    }
}
