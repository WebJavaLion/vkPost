package ru.vk.bot.repost.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Lev_S
 */

@Entity
@Table(name = "vk_attachment")
@NoArgsConstructor
@Data
@EqualsAndHashCode
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class VkAttachment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String url;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id")
    VkPost post;

    public VkAttachment(String url) {
        this.url = url;
    }
}
