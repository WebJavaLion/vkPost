package ru.vk.bot.repost.service;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.vk.bot.repost.entities.ChatManager;
import ru.vk.bot.repost.entities.VkAttachment;
import ru.vk.bot.repost.entities.VkPost;
import ru.vk.bot.repost.enums.PhotoSizeEnum;
import ru.vk.bot.repost.interfaces.Sender;
import ru.vk.bot.repost.interfaces.UpdateHandler;
import ru.vk.bot.repost.processor.PublishProcessor;
import ru.vk.bot.repost.repository.VkPostRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Lev_S
 */

@Service
@Data
@RequiredArgsConstructor
@Transactional
public class VkApiRequestService {

    Logger LOGGER = LoggerFactory.getLogger(VkApiRequestService.class);

    private final ServiceActor actor;

    private final VkApiClient client;

    private final VkPostRepository postRepository;

    private final TelegramBotService botService;

    private final static Pattern PATTERN = Pattern
            .compile("^.*(https://m\\.youtube|https://www\\.twitch\\.tv/|https://youtu\\.be/).*$");

    private static final Integer TARGET_GROUP_ID = -79268570; //-139228227;

    private static final Pattern VALIDATE_PATTERN = Pattern.compile("\\[club[0-9]+\\|[^]]+");

    private static final Integer countOfVkPosts = 5;

    public void createPost() {
        List<VkPost> postsForBd = new ArrayList<>();

        try {
            GetResponse response = client
                    .wall()
                    .get(actor)
                    .count(countOfVkPosts)
                    .ownerId(TARGET_GROUP_ID)
                    .execute();

            List<WallpostFull> postListFromResponse = response.getItems();

            Set<Integer> existingSetFromDb = postRepository
                    .findByVkIdIn(
                            postListFromResponse
                                    .stream()
                                    .map(WallpostFull::getId)
                                    .collect(Collectors.toList())
                    )
                    .stream()
                    .map(VkPost::getVkId)
                    .collect(Collectors.toSet());

            postListFromResponse
                    .stream()
                    .filter(
                            post ->
                                    (
                                            PATTERN.matcher(post.getText()).matches()
                                                    || (!post.getText().contains("http://") && !post.getText().contains("https://"))
                                                    && Arrays
                                                    .stream(post.getText().split("."))
                                                    .allMatch(
                                                            str -> !str.startsWith("ru")
                                                                    && !str.startsWith("com")
                                                                    && !str.startsWith("pro")
                                                                    && !str.startsWith("net")
                                                    )
                                                    && !post.isMarkedAsAds()
                                                    && CollectionUtils.isEmpty(post.getCopyHistory())
                                    )
                    )
                    .filter(
                            post -> {
                                if (!CollectionUtils.isEmpty(post.getAttachments())) {
                                    return !post.getAttachments()
                                            .stream()
                                            .map(wallPost -> wallPost.getType().getValue())
                                            .collect(Collectors.toList())
                                            .contains("video") || PATTERN.matcher(post.getText()).matches();
                                } else {
                                    return true;
                                }
                            }
                    ).forEach(post -> {
                if (!existingSetFromDb.contains(post.getId())) {
                    VkPost postForBd = new VkPost();

                    postForBd.setVkId(post.getId());
                    postForBd.setText(post.getText());
                    postForBd.setIsSent(false);
                    postForBd.setPreparedToPost(false);
                    postForBd.setDate(new Timestamp(new Date().getTime()));

                    List<VkAttachment> attachmentsForBd = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(post.getAttachments())) {

                        for (WallpostAttachment attachmentFromResponse : post.getAttachments()) {
                            VkAttachment attachmentForBd = new VkAttachment();
                            attachmentForBd.setPost(postForBd);

                            String attachmentType = attachmentFromResponse.getType().getValue();
                            if ("photo".equals(attachmentType)) {
                                Photo photo = attachmentFromResponse.getPhoto();

                                for (PhotoSizeEnum size : PhotoSizeEnum.values()) {
                                    if (attachmentForBd.getUrl() != null) {
                                        break;
                                    }
                                    photo.getSizes().stream()
                                            .filter(photoSize -> size.getValue().equals(photoSize.getType().getValue()))
                                            .findAny()
                                            .ifPresent(photoSize -> attachmentForBd.setUrl(photoSize.getUrl().getPath()));
                                }
                            } else if ("doc".equals(attachmentType)) {
                                attachmentForBd.setUrl(
                                        attachmentFromResponse
                                                .getDoc()
                                                .getUrl()
                                                .getPath()
                                );
                            }
                            if (!StringUtils.isEmpty(attachmentForBd.getUrl())) {
                                attachmentsForBd.add(attachmentForBd);
                            }
                        }
                        if (!attachmentsForBd.isEmpty()) {
                            postForBd.setPreparedToPost(true);
                        }

                        String[] splittedTextForReference = post.getText().split("https");
                        String referenceUrl = "";

                        if (splittedTextForReference.length == 2) {
                            if (splittedTextForReference[1].indexOf(' ') != -1) {
                                referenceUrl = "https" + splittedTextForReference[1]
                                        .substring(0, splittedTextForReference[1].indexOf(' '));
                            } else {
                                referenceUrl = "https" + splittedTextForReference[1];
                            }
                        }

                        VkAttachment referenceAttachment = new VkAttachment();
                        if (!StringUtils.isEmpty(referenceUrl)) {
                            referenceAttachment.setPost(postForBd);
                            referenceAttachment.setUrl(referenceUrl);
                            attachmentsForBd.add(referenceAttachment);
                        }
                    }
                    postForBd.setAttachments(attachmentsForBd);
                    postsForBd.add(postForBd);
                }
            });

            postRepository.saveAll(postsForBd);

            LOGGER.info("Posts with id's were created: "
                    + postsForBd.stream().map(VkPost::getVkId).collect(Collectors.toList()));

        } catch (ApiException | ClientException e) {
            e.printStackTrace();
        }
    }

    public void tryToUpdate() throws Exception {

        List<VkPost> allPostsWithoutAttachments =
                postRepository
                        .findAllWithoutAttachmentsAndPreparedFalse()
                        .stream()
                        .filter(p -> (Instant.now().getEpochSecond()
                                - p.getDate().toInstant().getEpochSecond()) > 600L)
                        .collect(Collectors.toList());

        LOGGER.info("Posts without attachments size is " + allPostsWithoutAttachments.size());

        if (!allPostsWithoutAttachments.isEmpty()) {

            List<WallpostFull> wallPostFulls =
                    client.wall()
                            .getById(actor,
                                    allPostsWithoutAttachments
                                            .stream()
                                            .map(VkPost::getVkId)
                                            .map(id -> TARGET_GROUP_ID.toString() + "_" + id.toString())
                                            .collect(Collectors.toList())
                            ).execute();

            wallPostFulls.forEach(post -> {
                if (post != null && !CollectionUtils.isEmpty(post.getAttachments())) {
                    List<String> collectionOfAttachmentsUrls = post.getAttachments()
                            .stream()
                            .filter(wallpostAttachment -> "photo".equals(wallpostAttachment.getType()
                                    .getValue()))
                            .map(att -> att.getPhoto()
                                    .getSizes()
                                    .stream()
                                    .filter(photoSize -> Arrays.stream(PhotoSizeEnum.values())
                                            .map(PhotoSizeEnum::getValue)
                                            .anyMatch(value -> value.equals(photoSize.getType().getValue())
                                            )
                                    )
                                    .min(Comparator.comparingInt(a -> Arrays.stream(PhotoSizeEnum.values())
                                            .map(PhotoSizeEnum::getValue)
                                            .collect(Collectors.toList())
                                            .indexOf(a.getType().getValue()))
                                    )
                                    .orElseThrow(RuntimeException::new)
                                    .getUrl()
                                    .getPath())
                            .collect(Collectors.toList());

                    if (collectionOfAttachmentsUrls.size() == 1) {
                        LOGGER.info(post.getText() + "\n" +
                                collectionOfAttachmentsUrls.get(0));

                        VkAttachment attachment = new VkAttachment();
                        attachment.setUrl(collectionOfAttachmentsUrls.get(0));

                        allPostsWithoutAttachments.stream()
                                .filter(p -> p.getVkId().equals(post.getId()))
                                .findFirst()
                                .ifPresent(p -> {
                                    p.setText(post.getText());
                                    p.setAttachments(new ArrayList<>(
                                            Collections.singletonList(attachment))
                                    );
                                    attachment.setPost(p);

                                });
                    } else {
                        LOGGER.info(post.getText() + "\n" +
                                collectionOfAttachmentsUrls);

                        allPostsWithoutAttachments.stream()
                                .filter(p -> p.getVkId().equals(post.getId()))
                                .findFirst()
                                .ifPresent(p -> {
                                        p.setText(post.getText());
                                        p.setAttachments(
                                                collectionOfAttachmentsUrls
                                                        .stream()
                                                        .map(VkAttachment::new)
                                                        .peek(att -> att.setPost(p))
                                                        .collect(Collectors.toList())
                                          );
                                });
                    }
                }
            });
            allPostsWithoutAttachments.forEach(p -> p.setPreparedToPost(true));
            postRepository.saveAll(allPostsWithoutAttachments);
        }
    }

    public String validateText(String text) {
        Matcher matcher = VALIDATE_PATTERN.matcher(text);

        while (matcher.find()) {
            String group = matcher.group() + ']';
            text = text.replace(
                    group,
                    group.substring(
                            group.indexOf('|') + 1,
                            group.length() - 1)
            );
        }
        return text;
    }

}
