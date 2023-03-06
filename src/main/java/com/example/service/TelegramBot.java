package com.example.service;

import com.example.config.BotConfig;
import com.example.dto.InnerCategoryDTO;
import com.example.entity.PostEntity;
import com.example.entity.UserEntity;
import com.example.enums.Action;
import com.example.enums.Label;
import com.example.enums.Language;
import com.example.enums.Role;
import com.example.utils.MD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;

    private final UsersService usersService;

    private final CategoryService categoryService;

    private final InnerCategoryService innerCategoryService;

    private final PostService postService;

    private final PostPhotoService postPhotoService;

    private final AdminHistoryService adminHistoryService;

    private final UserHistoryService userHistoryService;

    public TelegramBot(BotConfig config, UsersService usersService, CategoryService categoryService, InnerCategoryService innerCategoryService, PostPhotoService postPhotoService, PostService postService, AdminHistoryService adminHistoryService, UserHistoryService userHistoryService) {
        this.config = config;
        this.usersService = usersService;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Boshlash"));
        listOfCommands.add(new BotCommand("/lang", "Tilni tanlash"));
        listOfCommands.add(new BotCommand("/help", "Bot haqida ma'lumot"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error during setting bot's command list: {}", e.getMessage());
        }
        this.categoryService = categoryService;
        this.innerCategoryService = innerCategoryService;
        this.postPhotoService = postPhotoService;
        this.postService = postService;
        this.adminHistoryService = adminHistoryService;
        this.userHistoryService = userHistoryService;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            if (update.getMessage().getChat().getType().equals("supergroup")) {
                // DO NOTHING CHANNEL CHAT ID IS -1001764816733
            } else {
                if (update.hasMessage() && update.getMessage().hasText()) {
                    usersService.createUser(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                    Role role = usersService.getRoleByChatId(chatId);

                    String messageText = update.getMessage().getText();
                    if (messageText.startsWith("/send")) {
                        if (role == Role.ROLE_ADMIN) {
                            String textToSend = messageText.substring(messageText.indexOf(" "));
                            Iterable<UserEntity> all = usersService.getAll();
                            for (UserEntity entity : all) {
                                sendMessage(entity.getChatId(), textToSend);
                            }
                        }
                    } else if (messageText.startsWith("/")) {
                        switch (messageText) {
                            case "/start" -> {
                                startCommandReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                            }
                            case "/lang" -> {
                                startCommandReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());
                            }
                            case "/help" -> {
                                helpCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                            }
                            default -> sendMessage(chatId, "Sorry, command was not recognized");
                        }
                    }

                    Language lang = usersService.getLanguageByChatId(chatId);

                    Label lastLabelByChatId = userHistoryService.getLastLabelByChatId(chatId);

                    if (role == Role.ROLE_ADMIN) {
                        // last action was creating
                        Action lastAction = adminHistoryService.getLastAction(chatId);
                        Action lastOpened = adminHistoryService.getLastOpened(chatId);
                        Label lastLabel = adminHistoryService.getLastLabel(chatId);

                        if (lastOpened == Action.CATEGORY_OPENING) {
                                if (lastLabel.equals(Label.ASKING_STARTED)) {
                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.CATEGORY_NAME_UZ_ASKED, update.getMessage().getText());
                                    sendMessage(chatId, "Введите название категории..");
                                }
                                else if (lastLabel.equals(Label.CATEGORY_NAME_UZ_ASKED)) {
                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.CATEGORY_NAME_RU_ASKED, update.getMessage().getText());
                                    adminHistoryService.saveCategory();
                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.ASKING_FINISHED, "NO VALUE");

                                    SendMessage message = new SendMessage();
                                    message.setChatId(chatId);
                                    if (lang.equals(Language.UZ)) {
                                        message.setText("Kategoriya yaratildi!");
                                    } else {
                                        message.setText("Kategoriya yaratildi!");
                                    }

                                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                                    List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                                    InlineKeyboardButton btn = new InlineKeyboardButton();
                                    if (lang.equals(Language.UZ)) {
                                        btn.setText("Bosh Menyuga o'tish ➡️");
                                    } else {
                                        btn.setText("Перейти в главное меню ➡️");
                                    }
                                    btn.setCallbackData("admin");
                                    rowInLine.add(btn);

                                    rows.add(rowInLine);
                                    inlineKeyboardMarkup.setKeyboard(rows);
                                    message.setReplyMarkup(inlineKeyboardMarkup);

                                    try {
                                        execute(message);
                                    } catch (TelegramApiException e) {

                                    }

                                    return;
                                }
                            }
                        else if (lastOpened == Action.INNER_CATEGORY_OPENING) {
                                // CREATE INNER CATEGORY

                                String lastOpenedValue = adminHistoryService.getLastOpenedValue(chatId);
                                if (lastLabel.equals(Label.ASKING_STARTED)) {
                                    adminHistoryService.create(chatId, Action.INNER_CATEGORY_CREATING, Label.INNER_CATEGORY_NAME_UZ_ASKED, update.getMessage().getText());
                                    sendMessage(chatId, "Введите название категории..");
                                }
                                else if (lastLabel.equals(Label.INNER_CATEGORY_NAME_UZ_ASKED)) {
                                    adminHistoryService.create(chatId, Action.INNER_CATEGORY_CREATING, Label.INNER_CATEGORY_NAME_RU_ASKED, update.getMessage().getText());
                                    adminHistoryService.saveInnerCategory(lastOpenedValue);
                                    adminHistoryService.create(chatId, Action.INNER_CATEGORY_CREATING, Label.ASKING_FINISHED, "NO VALUE");

                                    SendMessage message = new SendMessage();
                                    message.setChatId(chatId);
                                    if (lang.equals(Language.UZ)) {
                                        message.setText("Kategoriya yaratildi!");
                                    } else {
                                        message.setText("Kategoriya yaratildi!");
                                    }

                                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                                    List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                                    InlineKeyboardButton btn = new InlineKeyboardButton();
                                    if (lang.equals(Language.UZ)) {
                                        btn.setText(lastOpenedValue + " ga o'tish ➡️");
                                    } else {
                                        btn.setText("Перейти в главное меню ➡️");
                                    }
                                    btn.setCallbackData("/inner " + lastOpenedValue);
                                    rowInLine.add(btn);

                                    rows.add(rowInLine);
                                    inlineKeyboardMarkup.setKeyboard(rows);
                                    message.setReplyMarkup(inlineKeyboardMarkup);

                                    try {
                                        execute(message);
                                    } catch (TelegramApiException e) {

                                    }
                                    return;
                                }
                            }
                        else if (lastOpened == Action.POST_CREATING) {
                            if (lastLabel == Label.ASKING_STARTED) {

                            }
                        }
                    }


                    if (innerCategoryService.findByName(messageText)) {
                        userHistoryService.create(Label.INNERCATEGORY_OPENED, chatId, messageText);
                        showPosts(chatId, messageText, role);
                    }
                    else if (categoryService.findByName(messageText)) {
                        userHistoryService.create(Label.CATEGORY_OPENED, chatId, messageText);
                        showInnerMenu(chatId, messageText, lang, role);
                    }
                    else if (messageText.equals("Chiqish")) {
                        // TODO Last Opened Main Menu
                        String lastCategoryName = userHistoryService.getLastCategoryName(chatId);
                        showInnerMenu(chatId, lastCategoryName, lang, role);
                    }
                    else if (messageText.equals("⬅️ Chiqish")) {
                        showMainMenu(chatId, lang, role);
                    }
                    else if (messageText.equals("+")) {
                        Action lastOpened = adminHistoryService.getLastOpened(chatId);
                        Label lastLabel = adminHistoryService.getLastLabel(chatId);

                        if (lastOpened == Action.CATEGORY_OPENING) {
                            adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.ASKING_STARTED, "NO VALUE");
                            sendMessage(chatId, "Kategoriya nomini kiriting..");
                        }
                        else if (lastOpened == Action.INNER_CATEGORY_OPENING) {
                            // CREATE INNER CATEGORY

                            String lastOpenedValue = adminHistoryService.getLastOpenedValue(chatId);

                            adminHistoryService.create(chatId, Action.INNER_CATEGORY_CREATING, Label.ASKING_STARTED, "NO VALUE");
                            sendMessage(chatId,  lastOpenedValue  + " nomini kiriting..");
                        }
                        else if (lastOpened == Action.POST_OPENING) {
                            // CREATE POST
                            adminHistoryService.create(chatId, Action.POST_CREATING, Label.ASKING_STARTED, "NO VALUE");
                            sendMessage(chatId,  "Menga postni jo'nating..");

                        }

//                        if (role.equals(Role.ROLE_ADMIN)) {
//                            Language language = usersService.getLanguageByChatId(chatId);
//                            Action lastAction = adminHistoryService.getLastAction(chatId);
//                            Label lastLabel = adminHistoryService.getLastLabel(chatId);
//
//                            // IS CATEGORY OPENED
//                            if (lastLabel == Label.ASKING_FINISHED) {
//
//                            }
//
//                            if (lastAction == Action.CATEGORY_CREATING) {
//
//                                if (lastLabel.equals(Label.ASKING_STARTED)) {
//                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.CATEGORY_NAME_UZ_ASKED, update.getMessage().getText());
//                                    sendMessage(chatId, "Введите название категории..");
//                                }
//                                else if (lastLabel.equals(Label.CATEGORY_NAME_UZ_ASKED)) {
//                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.CATEGORY_NAME_RU_ASKED, update.getMessage().getText());
//                                    adminHistoryService.saveCategory();
//                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.ASKING_FINISHED, "NO VALUE");
//
//                                    SendMessage message = new SendMessage();
//                                    message.setChatId(chatId);
//                                    if (language.equals(Language.UZ)) {
//                                        message.setText("Kategoriya yaratildi!");
//                                    } else {
//                                        message.setText("Kategoriya yaratildi!");
//                                    }
//
//                                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
//                                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
//                                    List<InlineKeyboardButton> rowInLine = new ArrayList<>();
//
//                                    InlineKeyboardButton btn = new InlineKeyboardButton();
//                                    if (language.equals(Language.UZ)) {
//                                        btn.setText("Bosh Menyuga o'tish ➡️");
//                                    } else {
//                                        btn.setText("Перейти в главное меню ➡️");
//                                    }
//                                    btn.setCallbackData("admin");
//                                    rowInLine.add(btn);
//
//                                    rows.add(rowInLine);
//                                    inlineKeyboardMarkup.setKeyboard(rows);
//                                    message.setReplyMarkup(inlineKeyboardMarkup);
//
//                                    try {
//                                        execute(message);
//                                    } catch (TelegramApiException e) {
//
//                                    }
//                                }
//                            }
//                            if (lastAction == Action.INNER_CATEGORY_CREATING) {
//                                Label lastLabel = adminHistoryService.getLastLabel(chatId);
//
//                                if (lastLabel.equals(Label.ASKING_STARTED)) {
//                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.CATEGORY_NAME_UZ_ASKED, update.getMessage().getText());
//                                    sendMessage(chatId, "Введите название категории..");
//                                } else if (lastLabel.equals(Label.CATEGORY_NAME_UZ_ASKED)) {
//                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.CATEGORY_NAME_RU_ASKED, update.getMessage().getText());
//                                    adminHistoryService.saveCategory();
//                                    adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.ASKING_FINISHED, "NO VALUE");
//
//                                    SendMessage message = new SendMessage();
//                                    message.setChatId(chatId);
//                                    if (language.equals(Language.UZ)) {
//                                        message.setText("Kategoriya yaratildi!");
//                                    } else {
//                                        message.setText("Kategoriya yaratildi!");
//                                    }
//
//                                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
//                                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
//                                    List<InlineKeyboardButton> rowInLine = new ArrayList<>();
//
//                                    InlineKeyboardButton btn = new InlineKeyboardButton();
//                                    if (language.equals(Language.UZ)) {
//                                        btn.setText("Bosh Menyuga o'tish ➡️");
//                                    } else {
//                                        btn.setText("Перейти в главное меню ➡️");
//                                    }
//                                    btn.setCallbackData("admin");
//                                    rowInLine.add(btn);
//
//                                    rows.add(rowInLine);
//                                    inlineKeyboardMarkup.setKeyboard(rows);
//                                    message.setReplyMarkup(inlineKeyboardMarkup);
//
//                                    try {
//                                        execute(message);
//                                    } catch (TelegramApiException e) {
//
//                                    }
//                                }
//                            }
//                        }

//                        else if (lastLabelByChatId == Label.CATEGORY_SHOWED) {
//                            userHistoryService.create(Label.CATEGORY_OPENED, chatId, messageText);
//                            showInnerMenu(chatId, messageText, lang, role);
//                        }
//                        else if (lastLabelByChatId == Label.INNERCATEGORY_SHOWED) {
//                            userHistoryService.create(Label.INNERCATEGORY_OPENED, chatId, messageText);
//                            showPosts(chatId, messageText);
//                        }

                        // If last asking finished
                    }

                    if (messageText.contains("login")) {
                        String token = update.getMessage().getText().substring(5);
                        if (token.trim().equals(MD5.md5("Grechka4kg"))) {
                            usersService.changeRole(chatId, Role.ROLE_ADMIN);
                            adminCommandReceived(chatId);
                        } else if (token.trim().equals(MD5.md5("Gashnich1bog"))) {
                            usersService.changeRole(chatId, Role.ROLE_OPERATOR);
                        }
                    }
                }

                else if (update.getMessage().hasPhoto()) {
                    Role role = usersService.getRoleByChatId(chatId);
                    Action lastOpened = adminHistoryService.getLastOpened(chatId);
                    Label lastLabel = adminHistoryService.getLastLabel(chatId);
                    String lastOpenedValue = adminHistoryService.getLastOpenedValue(chatId);

                    if (lastOpened == Action.POST_OPENING) {
                        // CREATE POST
                        if (lastLabel.equals(Label.ASKING_STARTED)) {
                            List<PhotoSize> photo = update.getMessage().getPhoto();

                            try {
                                GetFile getFile = new GetFile(photo.get(3).getFileId());
                                File tgFile = execute(getFile);
                                String fileUrl = tgFile.getFileUrl(getBotToken());
                                if (update.getMessage().getCaption()!=null) {
                                    postService.create(update.getMessage().getCaption(), innerCategoryService.getInnerCategoryIdByName(lastOpenedValue));
                                }
                                postPhotoService.create(postService.getLastId(), fileUrl);
                                System.out.println(fileUrl);
                                sendMessage(update.getMessage().getChatId(), fileUrl);
                            } catch (TelegramApiException e) {

                            }
                        }
                    }
                }
                else if (update.getMessage().hasContact()) {
                    // IS OFFER STARTED
                    Language language = usersService.getLanguageByChatId(chatId);

                    Long lastOfferId = Long.valueOf(userHistoryService.getLastOfferId(update.getMessage().getChatId()));
                    Contact contact = update.getMessage().getContact();

                    String firstName = contact.getFirstName();
                    String lastName = contact.getLastName();
                    String phoneNumber = contact.getPhoneNumber();


                    List<Long> chatIdByRole = usersService.getChatIdByRole(Role.ROLE_OPERATOR);
                    for (Long operatorId : chatIdByRole) {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(operatorId);
                        sendMessage.enableHtml(true);
                        Language operatorLanguage = usersService.getLanguageByChatId(chatId);

                        PostEntity post = postService.getPostById(lastOfferId);
                        if (operatorLanguage == Language.UZ) {
                            sendMessage.setText(
                                    "<b>" + post.getCategory().getNameUz() + "</b>" + " \n " +
                                            "\n" + "<i>" + post.getContent() + "</i>" +
                                            "\n" + firstName + " " + lastName + " " + phoneNumber);
                        }
                        else {
                            sendMessage.setText(
                                    "<b>" + post.getCategory().getNameRu() + "</b>" + " \n " +
                                            "\n" + "<i>" + post.getContent() + "</i>" +
                                            "\n" + firstName + " " + lastName + " " + phoneNumber);
                        }

                        try {
                            execute(sendMessage);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }


                    SendMessage message = new SendMessage();

                    message.setChatId(chatId);
                    message.setText("✅ <b>Bo'ldi. Operatorlarimiz siz bilan bog'lanadi.</b> \n ");
                    message.enableHtml(true);
                    try {
                        execute(message);
                    }
                    catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }

                    showMainMenu(chatId, language, Role.ROLE_USER);
                }
            }
        }
        else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();

            Language userLanguage = usersService.getLanguageByChatId(chatId);
            Role userRole = usersService.getRoleByChatId(chatId);

            String callBackData = update.getCallbackQuery().getData();
            if (callBackData.startsWith("LANG")) {
                if (callBackData.equals("LANG_UZ")) {
                    deleteMessageById(chatId, (int) messageId);
                    usersService.setLanguage(chatId, Language.UZ);
                    showMainMenu(chatId, Language.UZ, userRole);
                }
                else if (callBackData.equals("LANG_RU")) {
                    deleteMessageById(chatId, (int) messageId);
                    usersService.setLanguage(chatId, Language.RU);
                    showMainMenu(chatId, Language.RU, userRole);
                }
            }
            else if (callBackData.contains("SHOPPING")) {

                String postId = callBackData.substring(9);
                userHistoryService.create(Label.OFFER_STARTED, chatId, postId);

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Telefon raqamingizni kiriting..");
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> rows = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                KeyboardButton keyboardButton = new KeyboardButton();
                keyboardButton.setRequestContact(true);
                keyboardButton.setText("Telefon raqamni yuborish \uD83D\uDCF2");
                row.add(keyboardButton);
                rows.add(row);

                replyKeyboardMarkup.setSelective(true);
                replyKeyboardMarkup.setResizeKeyboard(true);
                replyKeyboardMarkup.setOneTimeKeyboard(true);
                replyKeyboardMarkup.setKeyboard(rows);

                sendMessage.setReplyMarkup(replyKeyboardMarkup);

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (callBackData.contains("/inner")) {
                String from = callBackData.substring(callBackData.indexOf(" ")).trim();
                showInnerMenu(chatId, from, userLanguage, userRole);
            }
            else if (callBackData.equals("ADD_CATEGORY")) {

            }
            else if (userLanguage == Language.UZ) {
                if (callBackData.equals("Chiqish")) {
//                    showMainMenu(chatId, (int) messageId, Language.UZ, userRole);
                }
                else if (callBackData.equals("admin")) {
                    showMainMenu(chatId, Language.UZ, userRole);
                }
                else if (callBackData.contains("inner")) {
                    String innerCategoryId = callBackData.substring(6);
                    List<PostEntity> postsByInnerCategoryId = postService.getPostsByInnerCategoryId(innerCategoryId);

                    for (PostEntity postEntity : postsByInnerCategoryId) {
                        deleteMessageById(chatId, (int) messageId);
                        sendPhotoPostMessage(chatId, postEntity.getContent(), postEntity.getId());
//                        messageHistoryService.create((int) messageId, chatId);

                    }
                }
                else {
//                    showInnerMenu(chatId, (int) messageId, callBackData, Language.UZ, userRole);
                }
            }
            else if (userLanguage == Language.RU) {
                if (callBackData.equals("Выход")) {
//                    showMainMenu(chatId, (int) messageId, Language.RU, userRole);
                }
                else if (callBackData.equals("admin")) {
                    showMainMenu(chatId, Language.RU, userRole);
                }
                else {
//                    showInnerMenu(chatId, (int) messageId, callBackData, Language.RU, userRole);
                }
            }
        }
    }

    private void showPosts(Long chatId, String from, Role role) {


        List<PostEntity> postsByInnerCategoryId = postService.getPostsByInnerCategoryId(from);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Maxsulotlar: " + postsByInnerCategoryId.size());
        message.enableHtml(true);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("Chiqish");
        if (role == Role.ROLE_ADMIN) {
            adminHistoryService.create(chatId, Action.POST_OPENING, Label.NO_LABEL, from);
            row.add("+");

        }

        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        for (PostEntity postEntity : postsByInnerCategoryId) {
            //postPhotoService.getPhotoUrl(postEntity.getId()).get(0)
            sendPhotoPostMessage(chatId, postEntity.getContent(),  postEntity.getId());
        }
    }

    private void adminCommandReceived(long chatId) {
        Language userLang = usersService.getLanguageByChatId(chatId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        if (userLang.equals(Language.UZ)) {
            message.setText("Siz endi adminsiz! ✅");
        } else if (userLang.equals(Language.RU)) {
            message.setText("Вы теперь админ! ✅");
        }
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        if (userLang.equals(Language.UZ)) {
            btn.setText("Administrator paneliga o'ting");
        } else if (userLang.equals(Language.RU)) {
            btn.setText("Перейти в панель администратора");
        }
        btn.setCallbackData("admin");
        rowInLine.add(btn);

        rows.add(rowInLine);

        inlineKeyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Something went wrong " + e);
        }
    }

    private void startCommandReceived(long chatId, String firstName, String lastName) {
        usersService.createUser(chatId, firstName, lastName);
        String answer = "<b>Assalomu alaykum! botga xush kelibsiz. Tilni tanlang</b>\n" +
                "\n" +
                "<b>Здравствуйте! Добро пожаловать в бота. Выберите язык</b>";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(answer);
        message.enableHtml(true);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton uzbekButton = new InlineKeyboardButton();
        uzbekButton.setText("\uD83C\uDDFA\uD83C\uDDFF O'zbekcha");
        uzbekButton.setCallbackData("LANG_UZ");
        rowInLine.add(uzbekButton);

        InlineKeyboardButton russianButton = new InlineKeyboardButton();
        russianButton.setText("\uD83C\uDDF7\uD83C\uDDFA Русский");
        russianButton.setCallbackData("LANG_RU");
        rowInLine.add(russianButton);

        rows.add(rowInLine);
        inlineKeyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    private void helpCommandReceived(long chatId, String name) {
        sendMessage(chatId, "Salom, " + name + "bu yerda bo't haqida ma'lumot bo'lishi kerak edi :)");
    }

    private void categoryAddCommandReceived(long chatId, Action action, Label label, Language language) {
        if (language == Language.UZ) {
            sendMessage(chatId, "Kategoriya nomini kiriting..");
        } else {
            sendMessage(chatId, "Введите название категории..");
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);
        message.enableHtml(true);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    private void showMainMenu(Long chatId, Language language, Role role) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
//        message.setMessageId(messageId);
        if (language == Language.UZ) {
            message.setText("<b>Bosh toifalar</b>");
        } else if (language == Language.RU) {
            message.setText("<b>Основные категории</b>");
        }
        message.enableHtml(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        List<String> contents;
        if (language == Language.UZ) {
            contents = categoryService.getAllUz();
        } else if (language == Language.RU) {
            contents = categoryService.getAllRu();
        } else {
            contents = categoryService.getAllUz();
        }

        for (int i = 0; i < contents.size(); i++) {
            if (i % 2 == 0 || i + 1 == contents.size()) {
                row = new KeyboardRow();
            }

            row.add(contents.get(i));

            if ((i != 0 && i % 2 == 1) || i + 1 == contents.size()) {
                rows.add(row);
            }
        }

        row = new KeyboardRow();
        if (role.equals(Role.ROLE_ADMIN)) {
            row.add(" + ");
        }

        rows.add(row);

        replyKeyboardMarkup.setKeyboard(rows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(replyKeyboardMarkup);

        if (role.equals(Role.ROLE_ADMIN)) {
            adminHistoryService.create(chatId, Action.CATEGORY_OPENING, Label.NO_LABEL, "NO_VALUE");
        }

        userHistoryService.create(Label.CATEGORY_OPENED, chatId, "NO_VALUE");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Please contact us. There is something went wrong " + e);
        }
    }

    private void showInnerMenu(Long chatId, String from, Language language, Role role) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
//        message.setMessageId(messageId);
        message.setText(from);


        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        List<InnerCategoryDTO> contents = null;
        if (language == Language.UZ) {
            contents = innerCategoryService.getAllUz(from);
        } else if (language == Language.RU) {
            contents = innerCategoryService.getAllRu(from);
        }

//        || contents.get(i).length() > 23
        for (int i = 0; i < contents.size(); i++) {
            if (i % 2 == 0 || (i + 1 == contents.size() && contents.size() > 2)) {
                row = new KeyboardRow();
            }
            row.add(contents.get(i).getName());

            if ((i != 0 && i % 2 == 1) || i + 1 == contents.size()) {
                rows.add(row);
            }
        }

        row = new KeyboardRow();
        if (role == Role.ROLE_ADMIN) {
            row.add(" + ");
            rows.add(row);

            adminHistoryService.create(chatId, Action.INNER_CATEGORY_OPENING, Label.NO_LABEL, from);
        }

        row = new KeyboardRow();
        if (language == Language.UZ) {
            row.add("⬅️ Chiqish");
            rows.add(row);
        } else {
            row.add("⬅️ Выход");
            rows.add(row);
        }

        replyKeyboardMarkup.setKeyboard(rows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(replyKeyboardMarkup);

        userHistoryService.create(Label.INNERCATEGORY_SHOWED, chatId, "NO_VALUE");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Something went wrong.. " + e);
        }
    }
    private void sendInnerMenu(Long chatId, String from, Language language, Role role) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(from);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();
        List<InnerCategoryDTO> contents = null;
        if (language == Language.UZ) {
            contents = innerCategoryService.getAllUz(from);
        } else if (language == Language.RU) {
            contents = innerCategoryService.getAllRu(from);
        }

//        || contents.get(i).length() > 23
        for (int i = 0; i < contents.size(); i++) {
            if (i % 2 == 0 || i + 1 == contents.size()) {
                rows = new ArrayList<>();
            }
            KeyboardRow row = new KeyboardRow();
            row.add(contents.get(i).getName());

            if ((i != 0 && i % 2 == 1) || i + 1 == contents.size()) {
                rows.add(row);
            }
        }

        rows = new ArrayList<>();
        KeyboardRow plusbtn = new KeyboardRow();
        if (role == Role.ROLE_ADMIN) {
            plusbtn.add(" + ");
            rows.add(plusbtn);

            adminHistoryService.create(chatId, Action.CATEGORY_OPENING, Label.NO_LABEL, from);
        }

        KeyboardRow button = new KeyboardRow();
        if (language == Language.UZ) {
            button.add("⬅️ Chiqish");
            rows.add(button);
        } else {
            button.add("⬅️ Выход");
            rows.add(button);
        }

        replyKeyboardMarkup.setKeyboard(rows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Something went wrong.. " + e);
        }
    }

    public void clearHistory(Update update) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(update.getMessage().getChatId());
            deleteMessage.setMessageId(update.getMessage().getMessageId());

            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void deleteMessageById(Long chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(messageId);

            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendPhotoPostMessage(Long chatId, String message, Long postId) {
        try {
            List<String> photoUrls = postPhotoService.getPhotoUrl(postId);

            List<InputMedia> photoList = new LinkedList<>();
            for (String photoUrl : photoUrls) {
//                InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(downloadFile(photoUrl));
//                System.out.println(inputMediaPhoto.getType());
                photoList.add(new InputMediaPhoto(photoUrl));
            }
            /*List<InputMedia> photoList = List.of(
                    new InputMediaPhoto("https://pixlr.com/images/index/remove-bg.webp"),
                    new InputMediaPhoto("https://pixlr.com/images/index/remove-bg.webp"),
                    new InputMediaPhoto("https://pixlr.com/images/index/remove-bg.webp")
            );*/

            if (!photoList.isEmpty()) {
                SendMediaGroup mediaGroup = new SendMediaGroup();
                mediaGroup.setMedias(photoList);
                mediaGroup.setChatId(chatId);
                execute(mediaGroup);
            }

            return;

           /* BufferedInputStream bis = new BufferedInputStream(new URL(imageUrl).openStream());
            SendPhoto photoMessage = new SendPhoto();
             photoMessage.setChatId(chatId);
            photoMessage.setCaption(message);
            photoMessage.setPhoto(new InputFile(bis, "image.jpg"));

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();


            InlineKeyboardButton uzbekButton = new InlineKeyboardButton();
            uzbekButton.setText("\uD83D\uDECD Buyurtma berish");
            uzbekButton.setCallbackData("SHOPPING " + postId);
            rowInLine.add(uzbekButton);

            rows.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rows);
            photoMessage.setReplyMarkup(inlineKeyboardMarkup);
//🛍
            // call the execute method of the BotsApi class to send the message
            execute(photoMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        } catch (RuntimeException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveImageFromUrl(String imageUrl, String savePath) {
        try {
            URL url = new URL(imageUrl);
            InputStream inputStream = url.openStream();
            Path saveFilePath = Paths.get(savePath);
            OutputStream outputStream = Files.newOutputStream(saveFilePath);

            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public File downloadFile(String fileUrl) {
//        java.io.File file = null;
//        try {
//
//            Properties sysProps = System.getProperties();
//            URL url = new URL(fileUrl);
//            InputStream in = url.openStream();
//            String directoryPath = sysProps.getProperty("file.separator") + sysProps.getProperty("user.home") + sysProps.getProperty("file.separator") + "Documents" + sysProps.getProperty("file.separator") + "dev";
//            java.io.File directory = new java.io.File(directoryPath);
//
//            String pathToFile = directoryPath + sysProps.getProperty("file.separator") + new Random().nextInt(100) + fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
//
//            if (!directory.exists()) {
//                directory.mkdirs();
//            }
//            file = new java.io.File(pathToFile);
//            file.createNewFile();
//
//            FileOutputStream outputStream = new FileOutputStream(file);
//            int read = 0;
//
//            byte[] bytes =  new byte[10000];
//            while ((read = in.read(bytes)) != -1) {
//                outputStream.write(bytes, 0, read);
//
//            }
//            outputStream.flush();
//            outputStream.close();
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return file;
//    }
}
