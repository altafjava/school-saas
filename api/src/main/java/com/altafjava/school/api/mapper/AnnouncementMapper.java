package com.altafjava.school.api.mapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AnnouncementDeliveryResponse;
import com.altafjava.school.api.dto.response.AnnouncementFeedItemResponse;
import com.altafjava.school.api.dto.response.AnnouncementRecipientResponse;
import com.altafjava.school.api.dto.response.AnnouncementResponse;
import com.altafjava.school.application.announcement.AnnouncementDeliveryReport;
import com.altafjava.school.application.announcement.AnnouncementService;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementChannel;
import com.altafjava.school.domain.announcement.model.AnnouncementRecipient;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AnnouncementMapper {

	@Mapping(target = "publicId", expression = "java(announcement.getPublicId().toString())")
	@Mapping(target = "audienceType", expression = "java(announcement.getAudienceType().name())")
	@Mapping(target = "status", expression = "java(announcement.getStatus().name())")
	@Mapping(target = "channels", source = "channels", qualifiedByName = "channelNames")
	@Mapping(target = "attachmentFilePublicIds", source = "attachmentFilePublicIds", qualifiedByName = "fileIds")
	@Mapping(target = "createdByUserPublicId", source = "createdByUserId", qualifiedByName = "user")
	@Mapping(target = "createdByUser", source = "createdByUserId", qualifiedByName = "userSummary")
	AnnouncementResponse toResponse(Announcement announcement);

	@Mapping(target = "publicId", expression = "java(item.announcement().getPublicId().toString())")
	@Mapping(target = "title", source = "announcement.title")
	@Mapping(target = "body", source = "announcement.body")
	@Mapping(target = "emergency", source = "announcement.emergency")
	@Mapping(target = "attachmentFilePublicIds", source = "announcement.attachmentFilePublicIds", qualifiedByName = "fileIds")
	@Mapping(target = "publishedAt", source = "announcement.publishedAt")
	@Mapping(target = "expiresAt", source = "announcement.expiresAt")
	AnnouncementFeedItemResponse toFeedItem(AnnouncementService.FeedItem item);

	@Mapping(target = "userPublicId", source = "userId", qualifiedByName = "user")
	@Mapping(target = "user", source = "userId", qualifiedByName = "userSummary")
	AnnouncementRecipientResponse toRecipient(AnnouncementRecipient recipient);

	default AnnouncementDeliveryResponse toDelivery(AnnouncementDeliveryReport report) {
		return new AnnouncementDeliveryResponse(report.recipientCount(), report.readCount(),
				report.recipientCount() - report.readCount(), report.dispatched(),
				report.channels().stream()
						.map(row -> new AnnouncementDeliveryResponse.ChannelDelivery(row.channel().name(),
								row.status().name(), row.skipReason() == null ? null : row.skipReason().name(),
								row.count()))
						.toList());
	}

	@Named("channelNames")
	default Set<String> channelNames(Set<AnnouncementChannel> channels) {
		return channels.stream().map(Enum::name).collect(Collectors.toSet());
	}

	@Named("fileIds")
	default List<String> fileIds(List<UUID> ids) {
		return ids.stream().map(UUID::toString).toList();
	}
}
