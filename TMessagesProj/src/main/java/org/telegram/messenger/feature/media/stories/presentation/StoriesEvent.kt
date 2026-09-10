package org.telegram.messenger.feature.media.stories.presentation

sealed class StoriesEvent {
    object Refresh : StoriesEvent()
    data class MarkAsRead(val dialogId: Long, val storyId: Int) : StoriesEvent()
    data class DeleteStory(val dialogId: Long, val storyId: Int) : StoriesEvent()
    data class TogglePin(val dialogId: Long, val storyId: Int, val pin: Boolean) : StoriesEvent()
    data class ToggleHide(val dialogId: Long, val hide: Boolean) : StoriesEvent()
    data class ActivateStealthMode(val future: Boolean, val past: Boolean) : StoriesEvent()
}
