if (window["showOrHideAttributeInput"] === undefined) {
    window.showOrHideAttributeInput = (prefix, uuid) => {
        const defaultChosen = (document.getElementById(prefix + uuid).selectedIndex == 0);

        document.getElementById("" + uuid).style.display = defaultChosen ? "block" : "none";
    };
}

Behaviour.specify("SELECT.xcode-show-hide-attr-input", "xcode-show-hide-attr-input", 0, (element) => {
    element.addEventListener("change", (event) => {
        const { prefix, uuid } = event.target.dataset;

        showOrHideAttributeInput(prefix, uuid);
    });
});
