package com.swiftlicious.hellblock.gui;

import com.swiftlicious.hellblock.gui.icon.BackToPageItem;

import xyz.xenondevs.invui.item.Item;

public interface ParentPage {

    void reOpen();

    default Item getBackItem() {
        return new BackToPageItem(this);
    }
}