document.addEventListener('DOMContentLoaded', function() {
    const config = document.querySelector(".jsplumb-config").dataset;

    window.depview.editEnabled = config.editEnabled === "true";
    window.depview.projectNameStripRegex = new RegExp(config.nameStripRegex, 'g');
    window.depview.projectNameStripRegexGroup = config.regexGroup;
});
