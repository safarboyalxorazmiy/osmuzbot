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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.io.File;

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

    private final AttachService attachService;

    @Value("${channel.id}")
    private Long channelId;

    public TelegramBot(BotConfig config, UsersService usersService, CategoryService categoryService, InnerCategoryService innerCategoryService, PostPhotoService postPhotoService, PostService postService, AdminHistoryService adminHistoryService, UserHistoryService userHistoryService, AttachService attachService) {
        this.config = config;
        this.usersService = usersService;
        this.attachService = attachService;

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
                return;
            } else {
                Role role = usersService.getRoleByChatId(chatId);

                if (role == Role.ROLE_OPERATOR) {
                    deleteMessageById(chatId, update.getMessage().getMessageId());
                    return;
                }

                if (update.hasMessage() && update.getMessage().hasText()) {
                    usersService.createUser(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getLastName());

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
                            case "/init" -> {
                                categoryService.init();

                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }

                                innerCategoryService.init();
                            }
                            default -> sendMessage(chatId, "Sorry, command was not recognized");
                        }
                    } else if (messageText.startsWith("+998")) {
                        // IS OFFER STARTED
                        Language language = usersService.getLanguageByChatId(chatId);

                        Long lastOfferId = Long.valueOf(userHistoryService.getLastOfferId(update.getMessage().getChatId()));

                        String phoneNumber = messageText;

                        List<Long> chatIdByRole = usersService.getChatIdByRole(Role.ROLE_OPERATOR);
                        for (Long operatorId : chatIdByRole) {
                            PostEntity post = postService.getPostById(lastOfferId);
                            String postPhoto = postPhotoService.getPhotoUrl(post.getId()).get(0);

                            sendMessageWithPhoto(String.valueOf(operatorId), postPhoto, "<b>Bo'lim: </b> " + post.getCategory().getNameUz() + " \n " +
                                    "\n" + "<i>" + post.getContent() + "</i> \n" +
                                    "\n " + phoneNumber);
                        }


                        SendMessage message = new SendMessage();

                        message.setChatId(chatId);
                        message.setText("✅ <b>Bo'ldi. Operatorlarimiz siz bilan bog'lanadi.</b> \n ");
                        message.enableHtml(true);
                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }

                        showMainMenu(chatId, language, role);
                    }

                    Language lang = usersService.getLanguageByChatId(chatId);

                    Label lastLabelByChatId = userHistoryService.getLastLabelByChatId(chatId);

                    if (role == Role.ROLE_ADMIN) {

                        if (messageText.equals("Saqlash")) {
                            // sendLastPostToChannel(); TODO Sending to channel canceled

                            SendMessage message = new SendMessage();
                            message.setChatId(chatId);
                            message.setText("Buyurtma yaratildi..");
                            message.enableHtml(true);
                            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                            List<KeyboardRow> keyboardRows = new ArrayList<>();
                            KeyboardRow row = new KeyboardRow();

                            if (lang == Language.UZ) {
                                row.add("🔙 Orqaga");
                            } else {
                                row.add("🔙 Назад");
                            }
                            row.add("+");

                            keyboardRows.add(row);
                            replyKeyboardMarkup.setKeyboard(keyboardRows);
                            replyKeyboardMarkup.setResizeKeyboard(true);
                            message.setReplyMarkup(replyKeyboardMarkup);

                            try {
                                execute(message);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        // last action was creating
                        Action lastAction = adminHistoryService.getLastAction(chatId);
                        Action lastOpened = adminHistoryService.getLastOpened(chatId);
                        Label lastLabel = adminHistoryService.getLastLabel(chatId);

                        if (lastOpened == Action.CATEGORY_OPENING) {
                            if (lastLabel.equals(Label.ASKING_STARTED)) {
                                adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.CATEGORY_NAME_UZ_ASKED, update.getMessage().getText());
                                sendMessage(chatId, "Введите название категории..");
                            } else if (lastLabel.equals(Label.CATEGORY_NAME_UZ_ASKED)) {
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
                        } else if (lastOpened == Action.INNER_CATEGORY_OPENING) {
                            // CREATE INNER CATEGORY

                            String lastOpenedValue = adminHistoryService.getLastOpenedValue(chatId);
                            if (lastLabel.equals(Label.ASKING_STARTED)) {
                                adminHistoryService.create(chatId, Action.INNER_CATEGORY_CREATING, Label.INNER_CATEGORY_NAME_UZ_ASKED, update.getMessage().getText());
                                sendMessage(chatId, "Введите название категории..");
                            } else if (lastLabel.equals(Label.INNER_CATEGORY_NAME_UZ_ASKED)) {
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
                        } else if (lastOpened == Action.POST_CREATING) {
                            if (lastLabel == Label.ASKING_STARTED) {

                            }
                        }
                    }


                    if (innerCategoryService.findByNameAndCategoryName(messageText, userHistoryService.getLastCategoryName( chatId))) {
                        userHistoryService.create(Label.INNERCATEGORY_OPENED, chatId, messageText);
                        String lastCategoryName = userHistoryService.getLastCategoryName(chatId);
                        showPosts(chatId, lastCategoryName, messageText, role, lang);
                    } else if (categoryService.findByName(messageText)) {
                        userHistoryService.create(Label.CATEGORY_OPENED, chatId, messageText);
                        showInnerMenu(chatId, messageText, lang, role);
                    } else if (messageText.equals("🔙 Orqaga")) {
                        String lastCategoryName = userHistoryService.getLastCategoryName(chatId);
                        showInnerMenu(chatId, lastCategoryName, lang, role);
                    } else if (messageText.equals("🔙 Назад")) {
                        String lastCategoryName = userHistoryService.getLastCategoryName(chatId);
                        showInnerMenu(chatId, lastCategoryName, lang, role);
                    } else if (messageText.equals("🔝 Asosiy Menyu")) {
                        showMainMenu(chatId, lang, role);
                    } else if (messageText.equals("🔝 Главное меню")) {
                        showMainMenu(chatId, lang, role);
                    } else if (messageText.equals("+")) {
                        Action lastOpened = adminHistoryService.getLastOpened(chatId);
                        Label lastLabel = adminHistoryService.getLastLabel(chatId);

                        if (lastOpened == Action.CATEGORY_OPENING) {
                            adminHistoryService.create(chatId, Action.CATEGORY_CREATING, Label.ASKING_STARTED, "NO VALUE");
                            sendMessage(chatId, "Kategoriya nomini kiriting..");
                        } else if (lastOpened == Action.INNER_CATEGORY_OPENING) {
                            String lastOpenedValue = adminHistoryService.getLastOpenedValue(chatId);

                            adminHistoryService.create(chatId, Action.INNER_CATEGORY_CREATING, Label.ASKING_STARTED, "NO VALUE");
                            sendMessage(chatId, lastOpenedValue + " nomini kiriting..");
                        } else if (lastOpened == Action.POST_OPENING) {
                            // CREATE POST
                            adminHistoryService.create(chatId, Action.POST_CREATING, Label.ASKING_STARTED, "NO VALUE");

                            SendMessage message = new SendMessage();

                            message.setChatId(chatId);
                            message.setText("Menga postni jo'nating..");
                            message.enableHtml(true);
                            message.enableMarkdown(true);

                            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                            List<KeyboardRow> rows = new ArrayList<>();

                            KeyboardRow row = new KeyboardRow();
                            KeyboardButton keyboardButton = new KeyboardButton();
                            keyboardButton.setText("Saqlash");
                            row.add(keyboardButton);
                            rows.add(row);

                            replyKeyboardMarkup.setSelective(true);
                            replyKeyboardMarkup.setResizeKeyboard(true);
                            replyKeyboardMarkup.setOneTimeKeyboard(true);
                            replyKeyboardMarkup.setKeyboard(rows);

                            message.setReplyMarkup(replyKeyboardMarkup);

                            try {
                                execute(message);
                            } catch (TelegramApiException e) {

                            }
                        }
                    } else if (messageText.equals("Kanal Sozlamalari ⚙️")) {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(chatId);
                        sendMessage.setText("Online Service Market kanali uchun sozlamalar \n");

                        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
                        InlineKeyboardButton btn = new InlineKeyboardButton();
                        btn.setText("Yangi maqola yaratish +");
                        btn.setCallbackData("+");
                        rowInLine.add(btn);

                        rows.add(rowInLine);

                        inlineKeyboardMarkup.setKeyboard(rows);
                        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

                        try {
                            execute(sendMessage);
                            return;
                        } catch (TelegramApiException e) {

                        }
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
                    if (role != Role.ROLE_ADMIN) {
                        deleteMessageById(chatId, update.getMessage().getMessageId());
                        return;
                    }

                    Action lastOpened = adminHistoryService.getLastOpened(chatId);
                    Label lastLabel = adminHistoryService.getLastLabel(chatId);
                    String lastOpenedValue = adminHistoryService.getLastOpenedValue(chatId);
                    String lastCategoryName = userHistoryService.getLastCategoryName(chatId);
                    Action lastAction = adminHistoryService.getLastAction(chatId);

                    if (lastAction == Action.CHANNEL_POST_CREATING) {
                        List<PhotoSize> photo = update.getMessage().getPhoto();

                        try {
                            GetFile getFile = new GetFile(photo.get(photo.size() - 1).getFileId());
                            org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFile);
                            String fileUrl = tgFile.getFileUrl(getBotToken());
                            String localUrl = attachService.saveImageFromUrl(fileUrl);
                            sendMessageToChannel(localUrl, update.getMessage().getCaption());
                            sendMessage(chatId, "Maqola yaratildi ✅");
                            adminHistoryService.create(chatId, Action.CHANNEL_POST_CREATED, Label.NO_LABEL, "NO_VALUE");
                        } catch (TelegramApiException ignored) {

                        }
                    } else if (lastOpened == Action.POST_OPENING) {
                        // CREATE POST
                        if (lastLabel.equals(Label.ASKING_STARTED)) {
                            List<PhotoSize> photo = update.getMessage().getPhoto();

                            try {
                                GetFile getFile = new GetFile(photo.get(photo.size() - 1).getFileId());
                                org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFile);
                                String fileUrl = tgFile.getFileUrl(getBotToken());
                                if (update.getMessage().getCaption() != null) {
                                    postService.create(update.getMessage().getCaption(), innerCategoryService.getInnerCategoryIdByNameAndCategoryName(lastOpenedValue, lastCategoryName));
                                }
                                String localUrl = attachService.saveImageFromUrl(fileUrl);
                                postPhotoService.create(postService.getLastId(), localUrl);
                                System.out.println(fileUrl);
                                System.out.println(localUrl);
                                sendMessage(chatId, fileUrl);
                            } catch (TelegramApiException e) {
                                log.warn("Something went wrong during uploading photo");
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
                        PostEntity post = postService.getPostById(lastOfferId);
                        String postPhoto = postPhotoService.getPhotoUrl(post.getId()).get(0);

                        sendMessageWithPhoto(String.valueOf(operatorId), postPhoto, "<b>Bo'lim: </b> " + post.getCategory().getNameUz() + " \n " +
                                "\n" + "<i>" + post.getContent() + "</i> \n" +
                                "\n" + firstName + " " + lastName + " " + phoneNumber);
                    }


                    SendMessage message = new SendMessage();

                    message.setChatId(chatId);
                    message.setText("✅ <b>Bo'ldi. Operatorlarimiz siz bilan bog'lanadi.</b> \n ");
                    message.enableHtml(true);
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }

                    showMainMenu(chatId, language, role);
                    return;
                }
            }
        } else if (update.hasCallbackQuery()) {
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
                } else if (callBackData.equals("LANG_RU")) {
                    deleteMessageById(chatId, (int) messageId);
                    usersService.setLanguage(chatId, Language.RU);
                    showMainMenu(chatId, Language.RU, userRole);
                }
            } else if (callBackData.contains("SHOPPING")) {

                String postId = callBackData.substring(9);
                userHistoryService.create(Label.OFFER_STARTED, chatId, postId);

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                if (userLanguage == Language.UZ) {
                    sendMessage.setText("Telefon raqamingizni kiriting..");
                } else {
                    sendMessage.setText("Введите свой номер телефона.");
                }
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> rows = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                KeyboardButton keyboardButton = new KeyboardButton();
                keyboardButton.setRequestContact(true);

                if (userLanguage == Language.UZ) {
                    keyboardButton.setText("Telefon raqamni yuborish \uD83D\uDCF2");
                } else {
                    keyboardButton.setText("Отправить номер телефона \uD83D\uDCF2");
                }

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
            } else if (callBackData.contains("/inner")) {
                String from = callBackData.substring(callBackData.indexOf(" ")).trim();
                showInnerMenu(chatId, from, userLanguage, userRole);
            } else if (callBackData.equals("+")) {
                if (userRole == Role.ROLE_ADMIN) {
                    sendMessage(chatId, "Menga postni jo'nating");
                    adminHistoryService.create(chatId, Action.CHANNEL_POST_CREATING, Label.NO_LABEL, "NO_VALUE");
                }
            } else if (callBackData.equals("ADD_CATEGORY")) {
            } else if (userLanguage == Language.UZ) {
                if (callBackData.equals("Chiqish")) {
//                    showMainMenu(chatId, (int) messageId, Language.UZ, userRole);
                } else if (callBackData.equals("admin")) {
                    showMainMenu(chatId, Language.UZ, userRole);
                } else if (callBackData.contains("inner")) {
                    String innerCategoryId = callBackData.substring(6);
                    String lastCategoryName = userHistoryService.getLastCategoryName(chatId);

                    List<PostEntity> postsByInnerCategoryId = postService.getPostsByInnerCategoryId(innerCategoryId, lastCategoryName);

                    for (PostEntity postEntity : postsByInnerCategoryId) {
                        deleteMessageById(chatId, (int) messageId);
                        sendPhotoPostMessage(chatId, postEntity.getContent(), postEntity.getId(), userLanguage);
//                        messageHistoryService.create((int) messageId, chatId);

                    }
                } else {
//                    showInnerMenu(chatId, (int) messageId, callBackData, Language.UZ, userRole);
                }
            } else if (userLanguage == Language.RU) {
                if (callBackData.equals("Выход")) {
//                    showMainMenu(chatId, (int) messageId, Language.RU, userRole);
                } else if (callBackData.equals("admin")) {
                    showMainMenu(chatId, Language.RU, userRole);
                } else {
//                    showInnerMenu(chatId, (int) messageId, callBackData, Language.RU, userRole);
                }
            }
        }
    }

    private void showPosts(Long chatId, String lastCategoryName, String lastInnerCategoryName, Role role, Language language) {
        List<PostEntity> postsByInnerCategoryId = postService.getPostsByInnerCategoryId(lastInnerCategoryName, lastCategoryName);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (language == Language.UZ) {
            message.setText("Maxsulotlar: " + postsByInnerCategoryId.size());
        } else {
            message.setText("Продукты: " + postsByInnerCategoryId.size());
        }
        message.enableHtml(true);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        if (language == Language.UZ) {
            row.add("🔙 Orqaga");
        } else {
            row.add("🔙 Назад");
        }
        if (role == Role.ROLE_ADMIN) {
            adminHistoryService.create(chatId, Action.POST_OPENING, Label.NO_LABEL, lastInnerCategoryName);
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
            sendPhotoPostMessage(chatId, postEntity.getContent(), postEntity.getId(), language);
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
        sendMessage(chatId, "Assalomu aleykum, " + name + ". \nSiz bu bot orqali o'zingizga kerakli bo'lgan bo'limlardan foydalanishingiz, buyurtma berishingiz, va o'z xizmatingizni taklif etishingiz mumkin. ");
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
            message.setText("<b>Hududni tanlang.</b>");
        } else if (language == Language.RU) {
            message.setText("<b>Выберите область.</b>");
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

        for (int i = 0; i <= contents.size() - 1; i++) {
            if (i % 2 == 0 || i + 1 == contents.size()) {
                row = new KeyboardRow();
            }

            // 4
            // 3 + 1 == 4

            row.add(contents.get(i));

            if ((i != 0 && i % 2 == 1) || i + 1 == contents.size()) {
                rows.add(row);
            }
        }

        row = new KeyboardRow();
        if (role.equals(Role.ROLE_ADMIN)) {
            row.add(" + ");
            row.add("Kanal Sozlamalari ⚙️");
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


/*        for (int i = 0; i <= contents.size() - 1; i++) {
            if (i % 2 == 0 || i + 1 == contents.size()) {
                row = new KeyboardRow();
            }

            // 4
            // 3 + 1 == 4

            row.add(contents.get(i));

            if ((i != 0 && i % 2 == 1) || i + 1 == contents.size()) {
                rows.add(row);
            }
        }

 */

//        || contents.get(i).length() > 23
        for (int i = 0; i <= contents.size() - 1; i++) {
            System.out.println(contents.get(i).getName());
            if (i % 2 == 0  || (i == contents.size())) {
                System.out.println("OPENING ROW");
                row = new KeyboardRow();

                System.out.println(contents.get(i).getName());
            }

            row.add(contents.get(i).getName());

            // 2
            if (i % 2 > 0 || (i == contents.size() - 1)) {
                System.out.println("CLOSING ROW");
                rows.add(row);

                System.out.println(contents.get(i).getName());
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
            row.add("🔝 Asosiy Menyu");
            rows.add(row);
        } else {
            row.add("🔝 Главное меню");
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

    public void sendPhotoPostMessage(Long chatId, String message, Long postId, Language language) {
        try {
            List<String> photoUrls = postPhotoService.getPhotoUrl(postId);
            File firstFile = new File(photoUrls.get(0));
            String extension = getExtension(firstFile.getName());
            if (extension.equalsIgnoreCase(".mp4")) {
                SendVideo sendVideo = new SendVideo();
                sendVideo.setChatId(chatId);
                sendVideo.setCaption(message);

                InputFile inputFile = new InputFile();
                inputFile.setMedia(firstFile, firstFile.getName());
                sendVideo.setVideo(inputFile);
                sendVideo.setParseMode("HTML");

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> rowInLine = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();

                if (language == Language.UZ) {
                    button.setText("\uD83D\uDECD Buyurtma berish");
                } else {
                    button.setText("\uD83D\uDECD Разместить заказ");
                }

                button.setCallbackData("SHOPPING " + postId);
                rowInLine.add(button);

                rows.add(rowInLine);
                inlineKeyboardMarkup.setKeyboard(rows);
                sendVideo.setReplyMarkup(inlineKeyboardMarkup);
                execute(sendVideo);
                return;
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setCaption(message);

            InputFile inputFile = new InputFile();
            inputFile.setMedia(firstFile, firstFile.getName());
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setParseMode("HTML");

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();

            InlineKeyboardButton button = new InlineKeyboardButton();

            if (language == Language.UZ) {
                button.setText("\uD83D\uDECD Buyurtma berish");
            } else {
                button.setText("\uD83D\uDECD Разместить заказ");
            }

            button.setCallbackData("SHOPPING " + postId);
            rowInLine.add(button);

            rows.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rows);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
            execute(sendPhoto);
        } catch (RuntimeException | TelegramApiException e) {
            log.warn("There is a problems during sending a media, {}", e);
        }
    }

    public void sendMessageToChannel(String photoUrl, String content) {
        try {
            File firstFile = new File(photoUrl);
            String extension = getExtension(firstFile.getName());
            if (extension.equalsIgnoreCase(".mp4")) {
                SendVideo sendVideo = new SendVideo();
                sendVideo.setChatId(channelId);
                sendVideo.setCaption(content);

                InputFile inputFile = new InputFile();
                inputFile.setMedia(firstFile, firstFile.getName());
                sendVideo.setVideo(inputFile);
                sendVideo.setParseMode("HTML");

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> rowInLine = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("\uD83D\uDECD Buyurtma berish");
                button.setUrl("https://t.me/osmuzbot");
                rowInLine.add(button);

                rows.add(rowInLine);
                inlineKeyboardMarkup.setKeyboard(rows);
                sendVideo.setReplyMarkup(inlineKeyboardMarkup);
                execute(sendVideo);
                return;
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(channelId);
            sendPhoto.setCaption(content);

            InputFile inputFile = new InputFile();
            inputFile.setMedia(firstFile, firstFile.getName());
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setParseMode("HTML");

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("\uD83D\uDECD Buyurtma berish");
            button.setUrl("https://t.me/osmuzbot");
            rowInLine.add(button);

            rows.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rows);
            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);
            execute(sendPhoto);
        } catch (RuntimeException | TelegramApiException e) {
            log.warn("There is a problems during sending a photos, {}", e);
        }
    }

    public String getExtension(String fileName) {
        if (fileName == null) {
            throw new RuntimeException("File name null");
        }
        int lastIndex = fileName.lastIndexOf(".");
        return fileName.substring(lastIndex + 1);
    }

    public void sendMessageWithPhoto(String chatId, String photoUrl, String caption) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(new File(photoUrl)));
        sendPhoto.setCaption(caption);
        sendPhoto.setParseMode("HTML");

        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
